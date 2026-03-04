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

    // create users
    @PostMapping("/register")
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

    // login
    @PostMapping("/login")
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

    @PostMapping("/logout")
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
    

    // update users - user tự update (authenticated users)
    @PatchMapping()
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Response<List<UserModel>>> updatesNormal(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid UpdateUserNormalRequest request
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("updatesNormal", request.getUserIds());
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
    public ResponseEntity<Response<List<UserModel>>> updatesForAdmin(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @RequestBody @Valid UpdateUserForAdminRequest request
    ){
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("updatesForAdmin", request.getUserIds());
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
    public ResponseEntity<Response<UserModel>> verifyAndActivate(
        @RequestHeader(value = "Accept-Language", defaultValue = "en") String acceptLanguage,
        @PathVariable Integer userId,
        @RequestParam(required = false) String code
    ) {
        Locale locale = Locale.forLanguageTag(acceptLanguage);
        LogContext logContext = getLogContext("verifyAndActivate", Collections.singletonList(userId));
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

}
