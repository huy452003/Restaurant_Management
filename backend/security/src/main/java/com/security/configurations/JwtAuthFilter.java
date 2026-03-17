package com.security.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.handle_exceptions.UnauthorizedExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;
import com.security.services.BlackListService;
import com.security.services.JwtService;

import java.util.List;
import java.io.IOException;
import java.util.Collections;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private LoggingService log;
    @Autowired
    private BlackListService blackListService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver handlerExceptionResolver;

    private LogContext getLogContext(String methodName, List<Integer> userIds) {
        return LogContext.builder()
            .module("security")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(userIds)
            .build();
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
            LogContext logContext = getLogContext("doFilterInternal", Collections.emptyList());
        log.logDebug("Processing request: " + request.getRequestURI(), logContext);

        String authHeader = request.getHeader("Authorization");
        String jwtToken;
        String username;

        // check nếu endpoint là public thì skip authentication
        if(isPublicEndpoint(request.getRequestURI())) {
            log.logDebug(
                "Public endpoint detected: " + request.getRequestURI()
                + " - Skipping authentication", logContext
            );
            filterChain.doFilter(request, response);
            return;
        }

        // check nếu header không có token hoặc token không bắt đầu bằng 'Bearer ' thì trả về lỗi Unauthorized
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.logDebug("No Authorization header or not Bearer token", logContext);
            UnauthorizedExceptionHandle e = new UnauthorizedExceptionHandle(
                "Missing or invalid Authorization header, it must start with 'Bearer '",
                "Security"
            );
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;
        }

        try {
            jwtToken = authHeader.substring(7);
            username = jwtService.extractUsername(jwtToken);
            log.logDebug("Extracted username from JWT: " + username, logContext);

            // check nếu token đã bị blacklist thì trả về lỗi Unauthorized
            if(blackListService.isBlackListed(jwtToken, username)){
                log.logWarn("Token is blacklisted, user " + username + " is logged out", logContext);
                UnauthorizedExceptionHandle e = new UnauthorizedExceptionHandle(
                    "User Token is blacklisted",
                    "Security"
                );
                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            }

            // nếu username không null và security context chưa set authentication thì tiến hành xác thực
            if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // lấy user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // kiểm tra token có hợp lệ không
                if(jwtService.isTokenValid(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                    // thêm vào security context
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.logDebug("Successfully authenticated user: " + username, logContext);
                }else {
                    // nếu token không hợp lệ thì trả về lỗi Unauthorized
                    log.logWarn("Invalid token for user: " + username, logContext);
                    UnauthorizedExceptionHandle e = new UnauthorizedExceptionHandle(
                        "Invalid token: " + jwtToken,
                        "Security"
                    );
                    handlerExceptionResolver.resolveException(request, response, null, e);
                    return;
                }
            }
        } catch (Exception e) {
            log.logError("Error processing JWT authentication", e, logContext);
            UnauthorizedExceptionHandle error = new UnauthorizedExceptionHandle(
                "Error processing JWT authentication",
                "Security"
            );
            handlerExceptionResolver.resolveException(request, response, null, error);
            return;
        }
        filterChain.doFilter(request, response);
    }

    // kiểm tra endpoint có phải là public không
    private boolean isPublicEndpoint(String uri) {
        return uri.endsWith("/register")
            || uri.endsWith("/login")
            || uri.endsWith("/logout")
            || uri.contains("/public/");
    }

}
