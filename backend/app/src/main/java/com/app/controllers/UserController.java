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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;

import com.app.services.UserService;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import jakarta.validation.Valid;

import com.common.models.UserModel;
import com.common.models.PaginatedResponse;
import com.common.models.security.RegisterModel;
import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import com.common.models.Response;
import com.common.models.security.LoginModel;
import com.common.models.security.UserSecurityModel;
import com.common.models.wrapper.UpdateUserForAdminRequest;
import com.common.models.wrapper.UpdateUserNormalRequest;
import com.handle_exceptions.TooManyRequestsExceptionHandle;

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
            .userIds(userIds)
            .build();
    }

    // get all users - chỉ ADMIN và MANAGER
    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @RateLimiter(name = "restaurant-management-read-controller", fallbackMethod = "getAllFallback")
    public ResponseEntity<Response<List<UserModel>>> getAll(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("getAll", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<UserModel> users = userService.getAll();
        Response<List<UserModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.getSuccess", null, locale),
            "userModel",
            null,
            users
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // login
    @PostMapping("/login")
    @RateLimiter(name = "restaurant-management-read-controller", fallbackMethod = "loginFallback")
    public ResponseEntity<Response<UserSecurityModel>> login(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid LoginModel req
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("login", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        UserSecurityModel loggedInUser = userService.login(req);
        Response<UserSecurityModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.loginSuccess", null, locale),
            "UserSecurityModel",
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
    public ResponseEntity<Response<List<UserSecurityModel>>> creates(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid List<RegisterModel> registers
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("creates", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<UserSecurityModel> createdUsers = userService.creates(registers);
        Response<List<UserSecurityModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createSuccess", null, locale),
            "UserSecurityModel",
            null,
            createdUsers
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    // update users - user tự update (authenticated users)
    @PatchMapping()
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "restaurant-management-write-controller", fallbackMethod = "updatesNormalFallback")
    public ResponseEntity<Response<List<UserModel>>> updatesNormal(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid UpdateUserNormalRequest request
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext(
            "updatesNormal", 
            request != null ? request.getUserIds() : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<UserModel> updatedUsers = userService.updatesNormal(request.getUpdates(), request.getUserIds());
        Response<List<UserModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateNormalSuccess", null, locale),
            "userModel",
            null,
            updatedUsers
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
    
    // verify and activate user - public
    @PutMapping("/public/{userId}/verify")
    @RateLimiter(name = "restaurant-management-write-controller", fallbackMethod = "verifyAndActivateFallback")
    public ResponseEntity<Response<UserModel>> verifyAndActivate(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @PathVariable Integer userId,
        @RequestParam(required = false) String code
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext(
            "verifyAndActivate",
            userId != null ? Collections.singletonList(userId) : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        // TODO: Nếu chưa có verification code system, có thể bỏ qua code parameter tạm thời
        String verificationCode = code != null ? code : "default"; // Tạm thời
        
        UserModel verifiedUser = userService.verifyAndActivate(userId, verificationCode);
        Response<UserModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.verifySuccess", new Object[]{"userModel"}, locale),
            "userModel",
            null,
            verifiedUser
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    //Fallback method *************************************************************//

    // getAllFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserModel>>> getAllFallback(
        String acceptLanguage, Exception e
    ) {
        LogContext logContext = getLogContext("getAllFallback", Collections.emptyList());

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for getAll endpoint", 
            "getAll"
        );
        log.logError("getAll controller is unavailable", error, logContext);
        throw error;
    }

    // loginFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserSecurityModel>> loginFallback(
        String acceptLanguage, LoginModel req, Exception e
    ) {
        LogContext logContext = getLogContext("loginFallback", Collections.emptyList());

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for login endpoint", 
            "login"
        );
        log.logError("login controller is unavailable", error, logContext);
        throw error;
    }

    // logoutFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<String>> logoutFallback(
        String acceptLanguage, String username, Exception e
    ) {
        LogContext logContext = getLogContext("logoutFallback", Collections.emptyList());

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for logout endpoint", 
            "logout"
        );
        log.logError("logout controller is unavailable", error, logContext);
        throw error;
    }

    // createsFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserSecurityModel>>> createsFallback(
        String acceptLanguage, List<RegisterModel> registers, Exception e
    ) {
        LogContext logContext = getLogContext("createsFallback", Collections.emptyList());

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for creates endpoint", 
            "creates"
        );
        log.logError("creates controller is unavailable", error, logContext);
        throw error;
    }
    
    // updatesNormalFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserModel>>> updatesNormalFallback(
        String acceptLanguage, UpdateUserNormalRequest request, Exception e
    ) {
        LogContext logContext = getLogContext(
            "updatesNormalFallback",
            request != null ? request.getUserIds() : Collections.emptyList()
        );

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for updatesNormal endpoint", 
            "updatesNormal"
        );
        log.logError("updatesNormal controller is unavailable", error, logContext);
        throw error;
    }

    // updatesForAdminFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserModel>>> updatesForAdminFallback(
        String acceptLanguage, UpdateUserForAdminRequest request, Exception e
    ) {
        LogContext logContext = getLogContext(
            "updatesForAdminFallback", 
            request != null ? request.getUserIds() : Collections.emptyList()
        );

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for updatesForAdmin endpoint", 
            "updatesForAdmin"
        );
        log.logError("updatesForAdmin controller is unavailable", error, logContext);
        throw error;
    }

    // filtersFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<PaginatedResponse<UserModel>>> filtersFallback(
        String acceptLanguage, Integer id, String username,
        String fullname, String email, String phone,
        Gender gender, LocalDate birth, String address,
        UserRole role, UserStatus userStatus, Pageable pageable, Exception e
    ){
        LogContext logContext = getLogContext("filtersFallback", Collections.emptyList());

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for filters endpoint", 
            "filters"
        );
        log.logError("filters controller is unavailable", error, logContext);
        throw error;
    }
    
    // verifyAndActivateFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserModel>> verifyAndActivateFallback(
        String acceptLanguage, Integer userId, String code, Exception e
    ) {
        LogContext logContext = getLogContext(
            "verifyAndActivateFallback", 
            userId != null ? Collections.singletonList(userId) : Collections.emptyList()
        );

        TooManyRequestsExceptionHandle error = new TooManyRequestsExceptionHandle(
            e.getMessage() != null ? e.getMessage() : "Rate limit exceeded for verifyAndActivate endpoint", 
            "verifyAndActivate"
        );
        log.logError("verifyAndActivate controller is unavailable", error, logContext);
        throw error;
    }

    //Fallback method *************************************************************//

}
