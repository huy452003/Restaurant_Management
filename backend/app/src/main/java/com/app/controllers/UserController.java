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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.common.models.user.LoginRequestModel;
import com.common.models.user.RegisterRequestModel;
import com.common.models.user.UpdateUserForAdminModel;
import com.common.models.user.UpdateUserNormalModel;
import com.common.models.user.UserModel;
import com.common.models.user.UserLoginModel;
import com.common.models.user.UserRegisterModel;
import com.common.models.user.UserUpdatePasswordRequestModel;
import com.common.models.PaginatedResponse;
import com.common.enums.Gender;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import com.common.models.Response;
import com.handle_exceptions.support.ResilienceFallbackUtils;
import com.common.models.wrapper.WrapperUpdateRequest;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import org.springframework.validation.annotation.Validated;

@RestController
@Validated
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
    @RateLimiter(name = "user-controller-staff-read", fallbackMethod = "filtersFallback")
    public ResponseEntity<Response<PaginatedResponse<UserModel>>> filters(
        Locale locale,
        @RequestParam(required = false) @Min(value = 1, message = "{validate.param.id.min}") Integer id,
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
            messageSource.getMessage("response.message.filterAndGetUsersSuccess", null, locale),
            "userModel",
            null,
            paginatedResponse
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // login
    @PostMapping("/login")
    @RateLimiter(name = "user-controller-login-logout", fallbackMethod = "loginFallback")
    public ResponseEntity<Response<UserLoginModel>> login(
        Locale locale,
        @RequestBody @Valid LoginRequestModel req
    ) {
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
    @PreAuthorize("isAuthenticated()")
    @RateLimiter(name = "user-controller-login-logout", fallbackMethod = "logoutFallback")
    public ResponseEntity<Response<String>> logout(Locale locale) {
        LogContext logContext = getLogContext("logout", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        userService.logout();
        
        Response<String> response = new Response<>(
            200,
            messageSource.getMessage("response.message.logoutSuccess", null, locale),
            "userModel",
            null,
            "Logged out successfully"
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // create users
    @PostMapping("/register")
    @RateLimiter(name = "user-controller-register-verify", fallbackMethod = "createsFallback")
    public ResponseEntity<Response<List<UserRegisterModel>>> creates(
        Locale locale,
        @RequestBody @Valid List<RegisterRequestModel> registers
    ) {
        LogContext logContext = getLogContext("creates", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<UserRegisterModel> createdUsers = userService.creates(registers);
        Response<List<UserRegisterModel>> response = new Response<>(
            201,
            messageSource.getMessage("response.message.createUsersSuccess", null, locale),
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
    @RateLimiter(name = "user-controller-customer-write", fallbackMethod = "updateNormalFallback")
    public ResponseEntity<Response<UserModel>> updateNormal(
        Locale locale,
        @RequestBody @Valid UpdateUserNormalModel update,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}") Integer userId
    ) {
        LogContext logContext = getLogContext(
            "updateNormal", 
            Collections.singletonList(userId)
        );

        log.logInfo("is running, preparing to call service ...!", logContext);

        UserModel updatedUser = userService.updateNormal(update, userId);
        Response<UserModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateUsersSuccess", null, locale),
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
    @RateLimiter(name = "user-controller-staff-write", fallbackMethod = "updatesForAdminFallback")
    public ResponseEntity<Response<List<UserModel>>> updatesForAdmin(
        Locale locale,
        @RequestBody @Valid WrapperUpdateRequest<UpdateUserForAdminModel> request
    ){
        LogContext logContext = getLogContext(
            "updatesForAdmin", 
            request != null ? request.getIds() : Collections.emptyList()
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        List<UserModel> updatedUsers = userService.updatesForAdmin(request.getUpdates(), request.getIds());
        Response<List<UserModel>> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updateUsersByAdminSuccess", null, locale),
            "UserModel",
            null,
            updatedUsers
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // update password by customer - chỉ CUSTOMER
    @PatchMapping("/{userId}/password")
    @PreAuthorize("hasRole('CUSTOMER')")
    @RateLimiter(name = "user-controller-customer-write", fallbackMethod = "updatePasswordByCustomerFallback")
    public ResponseEntity<Response<UserModel>> updatePasswordByCustomer(
        Locale locale,
        @RequestBody @Valid UserUpdatePasswordRequestModel update,
        @PathVariable @NotNull @Min(value = 1, message = "{validate.param.id.min}") Integer userId
    ){
        LogContext logContext = getLogContext(
            "updatePasswordByCustomer", 
            Collections.singletonList(userId)
        );
        log.logInfo("is running, preparing to call service ...!", logContext);

        UserModel updatedUser = userService.updatePasswordByCustomer(update, userId);
        Response<UserModel> response = new Response<>(
            200,
            messageSource.getMessage("response.message.updatePasswordByCustomerSuccess", null, locale),
            "UserModel",
            null,
            updatedUser
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
    
    // verify and activate user - public
    @PutMapping("/public/verify")
    @RateLimiter(name = "user-controller-register-verify", fallbackMethod = "verifyAndActivateFallback")
    public ResponseEntity<Response<UserModel>> verifyAndActivate(
        Locale locale,
        @RequestParam(required = false) String verificationToken
    ) {
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
    @RateLimiter(name = "user-controller-register-verify", fallbackMethod = "resendVerificationTokenFallback")
    public ResponseEntity<Response<String>> resendVerificationToken(
        Locale locale,
        @RequestParam @NotBlank(message = "validate.user.email.required") String email
    ) {
        LogContext logContext = getLogContext("resendVerificationToken", Collections.emptyList());
        log.logInfo("is running, preparing to call service ...!", logContext);

        userService.resendVerificationToken(email);
        Response<String> response = new Response<>(
            200,
            messageSource.getMessage("response.message.resendVerificationTokenSuccess", null, locale),
            "userModel",
            null,
            "Verification email sent to " + email
        );
        log.logInfo("completed, returning response ...!", logContext);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    // ======================================== Fallback Methods ========================================

    // filtersFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<PaginatedResponse<UserModel>>> filtersFallback(
        Locale locale, Integer id, String username,
        String fullname, String email, String phone,
        Gender gender, LocalDate birth, String address,
        UserRole role, UserStatus userStatus, Pageable pageable, Exception e
    ){
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "filters");
        return null;
    }
    
    // loginFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserLoginModel>> loginFallback(
        Locale locale, LoginRequestModel req, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "login");
        return null;
    }

    // logoutFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<String>> logoutFallback(
        Locale locale, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "logout");
        return null;
    }

    // createsFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserRegisterModel>>> createsFallback(
        Locale locale, List<RegisterRequestModel> registers, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "creates");
        return null;
    }
    
    // updateNormalFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserModel>> updateNormalFallback(
        Locale locale, UpdateUserNormalModel update, Integer userId, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "updateNormal");
        return null;
    }

    // updatesForAdminFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<List<UserModel>>> updatesForAdminFallback(
        Locale locale, WrapperUpdateRequest<UpdateUserForAdminModel> request, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "updatesForAdmin");
        return null;
    }
    
    // updatePasswordByCustomerFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserModel>> updatePasswordByCustomerFallback(
        Locale locale, UserUpdatePasswordRequestModel update, Integer userId, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "updatePasswordByCustomer");
        return null;
    }
    
    // verifyAndActivateFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<UserModel>> verifyAndActivateFallback(
        Locale locale, String verificationToken, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "verifyAndActivate");
        return null;
    }

    // resendVerificationTokenFallback
    @SuppressWarnings("unused")
    private ResponseEntity<Response<String>> resendVerificationTokenFallback(
        Locale locale, String email, Exception e
    ) {
        ResilienceFallbackUtils.propagateRateLimitFailure(e, "resendVerificationToken");
        return null;
    }

}
