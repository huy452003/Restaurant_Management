package com.app.controllers;

import java.util.List;
import java.util.Locale;
import java.time.LocalDate;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;

import com.app.services.UserService;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.validation.Valid;

import com.common.models.user.LoginRequestModel;
import com.common.models.user.RegisterRequestModel;
import com.common.models.user.UpdateUserNormalModel;
import com.common.models.user.UserModel;
import com.common.models.user.UserLoginModel;
import com.common.models.user.UserRegisterModel;
import com.common.models.PaginatedResponse;
import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import com.common.models.Response;
import com.common.models.wrapper.UpdateUserForAdminRequest;
import com.handle_exceptions.TooManyRequestsExceptionHandle;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.UnauthorizedExceptionHandle;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private LoggingService log;

    private LogContext getLogContext(String methodName, List<Integer> userIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(userIds)
            .build();
    }

    // filter and paginate users - chỉ ADMIN và MANAGER
    @GetMapping("/filterAndPaginate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @RateLimiter(name = "restaurant-management-read-controller", fallbackMethod = "filtersFallback")
    public ResponseEntity<Response<PaginatedResponse<UserModel>>> filters(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestParam(required = false) Integer id,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String fullname,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) Gender gender,
        @RequestParam(required = false) LocalDate birth,
        @RequestParam(required = false) String address,
        @RequestParam(required = false) UserRole role,
        @RequestParam(required = false) UserStatus userStatus,
        @PageableDefault(size = 5, sort = "id") Pageable pageable
    ){
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);
        
        // Gọi service method với Pageable (optional)
        Page<UserModel> userPage = userService.filters(
            id, username, fullname, 
            email, phone, gender, 
            birth, address, role, userStatus,
            pageable
        );

        PaginatedResponse<UserModel> paginatedResponse = PaginatedResponse.of(userPage);

        Response<PaginatedResponse<UserModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.filtersSuccess", null, locale),
            "userModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // login
    @PostMapping("/login")
    @RateLimiter(name = "restaurant-management-read-controller", fallbackMethod = "loginFallback")
    public ResponseEntity<Response<UserLoginModel>> login(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid LoginRequestModel req
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("login", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        UserLoginModel loggedInUser = userService.login(req);
        Response<UserLoginModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.loginSuccess", null, locale),
            "UserLoginModel",
            null,
            loggedInUser
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // logout
    @PostMapping("/logout")
    @RateLimiter(name = "restaurant-management-read-controller", fallbackMethod = "logoutFallback")
    public ResponseEntity<Response<String>> logout(

        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestParam String username
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("logout", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        userService.logout(username);
        
        Response<String> response = new Response<>(
            200,
            messageSource.getMessage("response.message.logoutSuccess", null, locale),
            "userModel",
            null,
            "username: " + username
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // create users
    @PostMapping("/register")
    @RateLimiter(name = "restaurant-management-write-controller", fallbackMethod = "createsFallback")
    public ResponseEntity<Response<List<UserRegisterModel>>> creates(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid List<RegisterRequestModel> registers
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("creates", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<UserRegisterModel> createdUsers = userService.creates(registers);
        Response<List<UserRegisterModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createSuccess", null, locale),
            "UserRegisterModel",
            null,
            createdUsers
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    // update user - user tự update thông tin của chính mình
    @PatchMapping("{userId}")
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "restaurant-management-write-controller", fallbackMethod = "updateNormalFallback")
    public ResponseEntity<Response<UserModel>> updateNormal(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid UpdateUserNormalModel update,
        @PathVariable Integer userId
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        
        LogContext logContext = getLogContext(
            "updateNormal", 
            Collections.singletonList(userId)
        );

        log.logInfo("is running, preparing to call service ...!", logContext);

        UserModel updatedUser = userService.updateNormal(update, userId);
        Response<UserModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateNormalSuccess", null, locale),
            "userModel",
            null,
            updatedUser
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // update users for admin - chỉ ADMIN
    @PutMapping()
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "restaurant-management-write-controller", fallbackMethod = "updatesForAdminFallback")
    public ResponseEntity<Response<List<UserModel>>> updatesForAdmin(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid UpdateUserForAdminRequest request
    ){
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext(
            "updatesForAdmin", 
            request != null ? request.getUserIds() : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<UserModel> updatedUsers = userService.updatesForAdmin(request.getUpdates(), request.getUserIds());
        Response<List<UserModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateForAdminSuccess", null, locale),
            "UserModel",
            null,
            updatedUsers
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    // verify and activate user - public
    @PutMapping("/public/verify")
    @RateLimiter(name = "restaurant-management-write-controller", fallbackMethod = "verifyAndActivateFallback")
    public ResponseEntity<Response<UserModel>> verifyAndActivate(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestParam(required = false) String verificationToken
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("verifyAndActivate", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        UserModel verifiedUser = userService.verifyAndActivate(verificationToken);
        Response<UserModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.verifySuccess", null, locale),
            "userModel",
            null,
            verifiedUser
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // resend verification token
    @PostMapping("/public/resendVerificationToken")
    @RateLimiter(name = "restaurant-management-write-controller", fallbackMethod = "resendVerificationTokenFallback")
    public ResponseEntity<Response<String>> resendVerificationToken(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestParam Integer userId
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("resendVerificationToken", Collections.singletonList(userId));
        log.logInfo("is running, preparing to call service ...!", logContext);

        String verificationToken = userService.resendVerificationToken(userId);
        Response<String> response = new Response<>(
            200,
            messageSource.getMessage("response.message.resendVerificationTokenSuccess", null, locale),
            "userModel",
            null,
            "verificationToken: " + verificationToken
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    //Fallback method *************************************************************//

    // filtersFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<PaginatedResponse<UserModel>>> filtersFallback(
        String acceptLanguage, Integer id, String username,
        String fullname, String email, String phone,
        Gender gender, LocalDate birth, String address,
        UserRole role, UserStatus userStatus, Pageable pageable, Exception e
    ){
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for filters endpoint", 
            "filters"
        );
        throw error;
    }
    
    // loginFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserLoginModel>> loginFallback(
        String acceptLanguage, LoginRequestModel req, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for login endpoint", 
            "login"
        );
        throw error;
    }

    // logoutFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<String>> logoutFallback(
        String acceptLanguage, String username, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for logout endpoint", 
            "logout"
        );
        throw error;
    }

    // createsFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserRegisterModel>>> createsFallback(
        String acceptLanguage, List<RegisterRequestModel> registers, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for creates endpoint", 
            "creates"
        );
        throw error;
    }
    
    // updateNormalFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserModel>> updateNormalFallback(
        String acceptLanguage, UpdateUserNormalModel update, Integer userId, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for updateNormal endpoint", 
            "updateNormal"
        );
        throw error;
    }

    // updatesForAdminFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserModel>>> updatesForAdminFallback(
        String acceptLanguage, UpdateUserForAdminRequest request, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for updatesForAdmin endpoint", 
            "updatesForAdmin"
        );
        throw error;
    }
    
    // verifyAndActivateFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserModel>> verifyAndActivateFallback(
        String acceptLanguage, String verificationToken, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for verifyAndActivate endpoint", 
            "verifyAndActivate"
        );
        throw error;
    }

    // resendVerificationTokenFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<String>> resendVerificationTokenFallback(
        String acceptLanguage, Integer userId, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Rate limit exceeded hoặc exception khác
        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for resendVerificationToken endpoint", 
            "resendVerificationToken"
        );
        throw error;
    }
    
    //Fallback method *************************************************************//

    // Helper method để check business exception
    private boolean isBusinessException(Exception e) {
        return e instanceof ConflictExceptionHandle || 
               e instanceof NotFoundExceptionHandle ||
               e instanceof ValidationExceptionHandle ||
               e instanceof ForbiddenExceptionHandle ||
               e instanceof UnauthorizedExceptionHandle;
    }

}
