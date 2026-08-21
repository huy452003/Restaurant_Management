package com.app.services.imp;

import com.app.services.EmailService;
import com.app.services.PendingUserRegistrationStore;
import com.app.services.UserService;
import com.app.utils.UserEntityUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.modelmapper.ModelMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.common.models.user.LoginRequestModel;
import com.common.models.user.RegisterRequestModel;
import com.common.models.user.UpdateUserForAdminModel;
import com.common.models.user.UpdateUserNormalModel;
import com.common.models.user.UserModel;
import com.common.models.user.UserLoginModel;
import com.common.models.user.PendingUserRegistration;
import com.common.models.user.UserRegisterModel;
import com.common.models.user.UserUpdatePasswordRequestModel;
import com.common.repositories.UserRepository;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import com.common.entities.UserEntity;
import com.common.utils.AgeUtils;
import com.common.utils.VietnamMobilePhoneUtils;
import com.common.utils.FilterPageCacheFacade;
import com.common.enums.Gender;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.handle_exceptions.UnauthorizedExceptionHandle;
import com.handle_exceptions.support.ResilienceFallbackUtils;
import com.security.configurations.JwtConfig;
import com.security.services.BlackListService;
import com.security.services.JwtService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor    
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;
    private final UserEntityUtils userEntityUtils;
    private final LoggingService log;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final BlackListService blackListService;
    private final PendingUserRegistrationStore pendingUserRegistrationStore;
    private final EmailService emailService;

    private LogContext getLogContext(String methodName, List<Integer> userIds){
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(userIds)
            .build();
    }

    private static final String USER_REDIS_KEY_PREFIX = "user:";

    private String formatExpirationTime(Long jwtExpirationTime){
        LocalDateTime expirationTime = LocalDateTime.now().plusSeconds(jwtExpirationTime / 1000);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return expirationTime.format(formatter);
    }

    // filter users with pagination
    @Override
    @CircuitBreaker(name = "user-service-read", fallbackMethod = "filtersFallback")
    public Page<UserModel> filters(
        Integer id, String username, String fullname,
        String email, String phone, Gender gender,
        LocalDate birth, String address, UserRole role, UserStatus userStatus,
        Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering users with pagination ...!", logContext);

        List<FilterCondition<UserEntity>> conditions = buildFilterConditions(
            id, username, fullname, email, phone, gender, birth, address, role, userStatus
        );

        // lấy cache key
        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            USER_REDIS_KEY_PREFIX, conditions, pageable);

        // lấy data từ cache
        Page<UserModel> cachedPage = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, UserModel.class);

        if (cachedPage != null && !cachedPage.isEmpty()) {
            log.logInfo("completed, found " + cachedPage.getTotalElements() + " users in cache", logContext);
            return cachedPage;
        }
        log.logInfo("Not found users in cache, filtering users with conditions: " + conditions, logContext);

        // lấy data từ db, nếu không có conditions thì getAll, nếu có thì filter theo conditions
        Page<UserEntity> pageEntities;
        if(conditions.isEmpty()){
            pageEntities = userRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all users with pagination", logContext);
        } else {
            Specification<UserEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = userRepository.findAll(spec, pageable);
        }
        
        // Map từ Entity sang Model và tính age
        List<UserModel> pageDatas = pageEntities.getContent().stream().map(
            this::toUserModel
        ).collect(Collectors.toList());
        
        // Tạo Page<UserModel> từ List<UserModel> và thông tin pagination từ Page<UserEntity>
        Page<UserModel> userModelPage = new PageImpl<>(
            pageDatas,                      // data 
            pageEntities.getPageable(),     // pageable (để lấy số trang hiện tại, số phần tử mỗi trang)
            pageEntities.getTotalElements() // total elements (để lấy tổng số phần tử)
        );

        // lưu cache
        if (redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, userModelPage);
            log.logInfo("Cached first-page filter snapshot for " + userModelPage.getTotalElements()
                + " users, key: " + redisKeyFilters, logContext);
        }

        log.logInfo("completed, found " + userModelPage.getTotalElements() + " users in " + 
                    userModelPage.getTotalPages() + " pages", logContext);
        return userModelPage;
    }

    // login
    @Override
    @CircuitBreaker(name = "user-service-login-logout", fallbackMethod = "loginFallback")
    public UserLoginModel login(LoginRequestModel req) {
        LogContext logContext = getLogContext("login", Collections.emptyList());
        log.logInfo("Logging in user ...!", logContext);

        // xác thực username và password
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (BadCredentialsException ex) {
            UnauthorizedExceptionHandle e = new UnauthorizedExceptionHandle(
                "Invalid username or password",
                "UserLoginModel"
            );
            log.logError(e.getMessage(), ex, logContext);
            throw e;
        }

        // query từ db
        UserEntity userEntity = userRepository.findByUsername(req.getUsername()).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "User not found with username: " + req.getUsername() + "when login",
                Collections.emptyList(),
                "UserLoginModel"
            );
            log.logError(e.getMessage(), null, logContext);
            return e;
        });

        // kiểm tra user status phải là ACTIVE
        if(userEntity.getUserStatus() == UserStatus.INACTIVE){
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "User is inactive",
                "UserLoginModel",
                "userStatus must be ACTIVE"
            );
            log.logError(e.getMessage(), null, logContext);
            throw e;
        }
        if(userEntity.getUserStatus() == UserStatus.PENDING){
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "User is pending",
                "UserLoginModel",
                "userStatus must be ACTIVE"
            );
            log.logError(e.getMessage(), null, logContext);
            throw e;
        }
        
        // trả response với tokens
        UserLoginModel userLoginModel = modelMapper.map(userEntity, UserLoginModel.class);
        Map<String, Object> claim = new HashMap<>();
        claim.put("role", userEntity.getRole().name());
        userLoginModel.setAccessToken(jwtService.generateAccessToken(claim, userEntity));
        userLoginModel.setExpires(formatExpirationTime(jwtConfig.expiration()));
        userLoginModel.setRefreshToken(jwtService.generateRefreshToken(claim, userEntity));
        userLoginModel.setRefreshExpires(formatExpirationTime(jwtConfig.refreshExpiration()));
        if(userEntity.getBirth() != null){
            userLoginModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
        }
        log.logInfo("completed, logged in user: " + userEntity.getUsername(), logContext);
        return userLoginModel;
    }

    // logout
    @Override
    @CircuitBreaker(name = "user-service-login-logout", fallbackMethod = "logoutFallback")
    public void logout() {
        LogContext logContext = getLogContext("logout", Collections.emptyList());
        log.logInfo("User is logging out ...!", logContext);

        UserEntity currentUser = userEntityUtils.requireAuthenticatedUser(
            "userModel", logContext
        );

        // thêm token vào blacklist (revoke token)
        blackListService.blackListUser(currentUser.getUsername());
        log.logInfo("Completed, logged out user: " + currentUser.getUsername() + " and blacklisted token", logContext);
    }

    // create users — lưu tạm Redis; chỉ ghi DB khi verify
    @Override
    @CircuitBreaker(name = "user-service-write", fallbackMethod = "createsFallback")
    public List<UserRegisterModel> creates(List<RegisterRequestModel> registers) {
        LogContext logContext = getLogContext("creates", Collections.emptyList());
        log.logInfo("Staging " + registers.size() + " registration(s) in Redis ...!", logContext);

        boolean hasAdminInSystem = userRepository.existsByRole(UserRole.ADMIN);
        for (RegisterRequestModel register : registers) {
            if (register.getRole().equals(UserRole.ADMIN) && hasAdminInSystem) {
                ValidationExceptionHandle e = new ValidationExceptionHandle(
                    "Admin role already exists",
                    Collections.singletonList(register.getRole()),
                    "UserRegisterModel"
                );
                log.logError("System already has an admin user, cannot create another admin", e, logContext);
                throw e;
            }
        }

        List<Object> conflicts = new ArrayList<>();
        for (RegisterRequestModel register : registers) {
            register.setUsername(register.getUsername().toLowerCase().trim());
            register.setEmail(register.getEmail().toLowerCase().trim());
            register.setPhone(VietnamMobilePhoneUtils.normalize(register.getPhone()));

            addRegistrationConflict(
                conflicts, "username", register.getUsername(),
                userRepository.existsByUsername(register.getUsername()),
                pendingUserRegistrationStore.existsByUsername(register.getUsername()),
                "Username already exists",
                "Username is pending verification"
            );

            addRegistrationConflict(
                conflicts, "email", register.getEmail(),
                userRepository.existsByEmail(register.getEmail()),
                pendingUserRegistrationStore.existsByEmail(register.getEmail()),
                "Email already exists",
                "Email is pending verification"
            );

            addRegistrationConflict(
                conflicts, "phone", register.getPhone(),
                userRepository.existsByPhone(register.getPhone()),
                pendingUserRegistrationStore.existsByPhone(register.getPhone()),
                "Phone already exists",
                "Phone is pending verification"
            );
        }

        if (!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "UserRegisterModel"
            );
            log.logError("Thrown an exception when staging registrations", e, logContext);
            throw e;
        }

        List<UserRegisterModel> results = new ArrayList<>();
        for (RegisterRequestModel register : registers) {
            String registrationId = UUID.randomUUID().toString();
            PendingUserRegistration pending = new PendingUserRegistration(
                registrationId,
                register.getUsername(),
                passwordEncoder.encode(register.getPassword()),
                register.getFullname(),
                register.getEmail(),
                register.getPhone(),
                register.getGender(),
                register.getBirth(),
                register.getAddress(),
                register.getRole()
            );
            pendingUserRegistrationStore.save(pending);

            emailService.sendVerificationEmail(register.getEmail(), jwtService.generateVerificationToken(registrationId));

            UserRegisterModel userRegisterModel = modelMapper.map(register, UserRegisterModel.class);
            userRegisterModel.setUserStatus(UserStatus.PENDING);
            if (register.getBirth() != null) {
                userRegisterModel.setAge(AgeUtils.calculateAge(register.getBirth()));
            }
            results.add(userRegisterModel);
        }

        log.logInfo("completed, staged " + results.size() + " registration(s) in Redis", logContext);
        return results;
    }

    // update user normal - user tự update thông tin của chính mình
    @Override
    @CircuitBreaker(name = "user-service-register-verify", fallbackMethod = "updateNormalFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public UserModel updateNormal(UpdateUserNormalModel update, Integer userId) {
        LogContext logContext = getLogContext(
            "updateNormal", 
            userId != null ? Collections.singletonList(userId) : Collections.emptyList()
        );       
        log.logInfo("User with id " + userId + " is updating their profile ...!", logContext);

        // lấy user đang đăng nhập
        UserEntity currentUser = userEntityUtils.requireAuthenticatedUserById(
            userId, "UserModel", logContext
        );

        update.setEmail(update.getEmail().toLowerCase().trim());
        update.setPhone(VietnamMobilePhoneUtils.normalize(update.getPhone()));

        // Kiểm tra trùng lặp trước khi cập nhật
        List<Object> conflicts = new ArrayList<>();
        
        // Check email: CHỈ check duplicate khi email THAY ĐỔI (khác với email hiện tại)
        if (!Objects.equals(update.getEmail(), currentUser.getEmail())) {
            if (userRepository.existsByEmail(update.getEmail())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "email");
                conflict.put("value", update.getEmail());
                conflict.put("message", "Email already exists for another user");
                conflicts.add(conflict);
            }
        }
        
        // Check phone: CHỈ check duplicate khi phone THAY ĐỔI (khác với phone hiện tại)
        if (!Objects.equals(update.getPhone(), currentUser.getPhone())) {
            if (userRepository.existsByPhone(update.getPhone())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "phone");
                conflict.put("value", update.getPhone());
                conflict.put("message", "Phone already exists for another user");
                conflicts.add(conflict);
            }
        }

        if (!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "userModel"
            );
            log.logError("Thrown an exception when update user normal", e, logContext);
            throw e;
        }

        // Check xem có thay đổi gì không
        boolean hasChanges = !Objects.equals(update.getFullname(), currentUser.getFullname()) ||
                             !Objects.equals(update.getEmail(), currentUser.getEmail()) ||
                             !Objects.equals(update.getPhone(), currentUser.getPhone()) ||
                             !Objects.equals(update.getGender(), currentUser.getGender()) ||
                             !Objects.equals(update.getBirth(), currentUser.getBirth()) ||
                             !Objects.equals(update.getAddress(), currentUser.getAddress());
        
        if (hasChanges) {
            // Map các field từ update model vào UserEntity
            modelMapper.map(update, currentUser);
            userRepository.saveAndFlush(currentUser);
            log.logInfo("completed, updated user with id: " + userId, logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        // xóa cache filter
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, USER_REDIS_KEY_PREFIX);
        log.logInfo("Deleted filter caches after update", logContext);
        
        // Return user đã update
        return toUserModel(currentUser);
    }

    // update user for admin
    @Override
    @CircuitBreaker(name = "user-service-admin-write", fallbackMethod = "updatesForAdminFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public List<UserModel> updatesForAdmin(List<UpdateUserForAdminModel> updates, List<Integer> userIds) {
        LogContext logContext = getLogContext(
            "updatesForAdmin", 
            userIds != null ? userIds : Collections.emptyList()
        );
        log.logInfo("Updating User from admin ...!", logContext);

        // kiểm tra số lượng updates phải bằng số lượng userIds
        if(updates.size() != userIds.size()){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and userIds",
                userIds,
                "UserModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        // tìm các user có trong userIds
        List<UserEntity> foundUsers = userIds.stream()
            .map(id -> userEntityUtils.requireById(id, "UserModel", logContext))
            .collect(Collectors.toList());

        // Check nếu có user muốn update role thành ADMIN nhưng đã có ADMIN khác trong hệ thống
        // chỉ khi user hiện tại là admin hoặc đang không có admin trong hệ thống -> bỏ qua check
        boolean hasAdminInSystem = userRepository.existsByRole(UserRole.ADMIN);
        for (int i = 0; i < updates.size(); i++) {
            UpdateUserForAdminModel update = updates.get(i);
            UserEntity currentUser = foundUsers.get(i);
            
            if (update.getRole().equals(UserRole.ADMIN) && 
                !currentUser.getRole().equals(UserRole.ADMIN) && 
                hasAdminInSystem) {
                ValidationExceptionHandle e = new ValidationExceptionHandle(
                    "Admin role already exists",
                    Collections.singletonList(update.getRole()),
                    "UserModel"
                );
                log.logError("System already has an admin user, cannot update another user to admin", e, logContext);
                throw e;
            }
        }

        // kiểm tra trùng lặp trước khi cập nhật
        List<Object> conflicts = new ArrayList<>();
        for( int i = 0; i < updates.size(); i++) {
            UpdateUserForAdminModel update = updates.get(i);
            UserEntity currentUser = foundUsers.get(i);

            update.setUsername(update.getUsername().toLowerCase().trim());
            update.setEmail(update.getEmail().toLowerCase().trim());
            update.setPhone(VietnamMobilePhoneUtils.normalize(update.getPhone()));

            if(!Objects.equals(update.getUsername(), currentUser.getUsername())){
                if(userRepository.existsByUsername(update.getUsername())){
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "username");
                    conflict.put("value", update.getUsername());
                    conflict.put("message", "Username already exists for another user");
                    conflicts.add(conflict);
                }
            }
            if(!Objects.equals(update.getEmail(), currentUser.getEmail())){
                if(userRepository.existsByEmail(update.getEmail())){
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "email");
                    conflict.put("value", update.getEmail());
                    conflict.put("message", "Email already exists for another user");
                    conflicts.add(conflict);
                }
            }
            if(!Objects.equals(update.getPhone(), currentUser.getPhone())){
                if(userRepository.existsByPhone(update.getPhone())){
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "phone");
                    conflict.put("value", update.getPhone());
                    conflict.put("message", "Phone already exists for another user");
                    conflicts.add(conflict);
                }
            }
        }

        if(!conflicts.isEmpty()){
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "UserModel"
            );
            log.logError("Thrown an exception when update user for admin", e, logContext);
            throw e;
        }

        // Map các field từ update model vào UserEntity đã tồn tại (giữ nguyên các field khác)
        // Chỉ update những user có thay đổi thực sự
        List<UserEntity> usersToUpdate = new ArrayList<>();
        Iterator<UpdateUserForAdminModel> updateIterator = updates.iterator();
        Iterator<UserEntity> userIterator = foundUsers.iterator();
        while(updateIterator.hasNext() && userIterator.hasNext()){
            UpdateUserForAdminModel update = updateIterator.next();
            UserEntity currentUser = userIterator.next();

            String newPasswordPlain = StringUtils.hasText(update.getPassword())
                ? update.getPassword().trim()
                : null;
            update.setPassword(null);

            boolean hasChanges = !Objects.equals(update.getUsername(), currentUser.getUsername()) ||
                                 !Objects.equals(update.getFullname(), currentUser.getFullname()) ||
                                 !Objects.equals(update.getEmail(), currentUser.getEmail()) ||
                                 !Objects.equals(update.getPhone(), currentUser.getPhone()) ||
                                 !Objects.equals(update.getGender(), currentUser.getGender()) ||
                                 !Objects.equals(update.getBirth(), currentUser.getBirth()) ||
                                 !Objects.equals(update.getAddress(), currentUser.getAddress()) ||
                                 !Objects.equals(update.getRole(), currentUser.getRole()) ||
                                 !Objects.equals(update.getUserStatus(), currentUser.getUserStatus()) ||
                                 newPasswordPlain != null;

            if(hasChanges){
                modelMapper.map(update, currentUser);
                if (newPasswordPlain != null) {
                    currentUser.setPassword(passwordEncoder.encode(newPasswordPlain));
                }
                usersToUpdate.add(currentUser);
            }
        }

        if(!usersToUpdate.isEmpty()){
            userRepository.saveAllAndFlush(usersToUpdate);
            log.logInfo("completed, updated " + usersToUpdate.size() + " users by admin", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        // xóa cache filter
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, USER_REDIS_KEY_PREFIX);
        log.logInfo("Deleted filter caches after update", logContext);

        // Return tất cả users (bao gồm cả những user không có thay đổi)
        return foundUsers.stream().map(
            this::toUserModel
        ).collect(Collectors.toList());
    }

    // update password by customer
    @Override
    @CircuitBreaker(name = "user-service-write", fallbackMethod = "updatePasswordByCustomerFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public UserModel updatePasswordByCustomer(UserUpdatePasswordRequestModel update, Integer userId) {
        LogContext logContext = getLogContext("updatePasswordByCustomer", Collections.singletonList(userId));
        log.logInfo("User with id " + userId + " is updating their password ...!", logContext);

        // lấy user đang đăng nhập
        UserEntity currentUser = userEntityUtils.requireAuthenticatedUserById(
            userId, "UserModel", logContext
        );

        // kiểm tra mật khẩu cũ phải giống với mật khẩu hiện tại
        if(!passwordEncoder.matches(update.getOldPassword(), currentUser.getPassword())){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Old password is incorrect",
                Collections.singletonList(userId),
                "UserModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        // mật khẩu mới và xác nhận phải trùng nhau
        if (!Objects.equals(update.getNewPassword(), update.getConfirmNewPassword())) {
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "New password and confirm password do not match",
                Collections.singletonList(userId),
                "UserModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        // kiểm tra mật khẩu mới phải khác với mật khẩu hiện tại
        if(passwordEncoder.matches(update.getNewPassword(), currentUser.getPassword())){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "New password is the same as the old password",
                Collections.singletonList(userId),
                "UserModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        currentUser.setPassword(passwordEncoder.encode(update.getNewPassword()));
        userRepository.saveAndFlush(currentUser);
        log.logInfo("completed, updated password for user with id: " + userId, logContext);
        return toUserModel(currentUser);
    }

    // verify and activate user
    @Override
    @CircuitBreaker(name = "user-service-register-verify", fallbackMethod = "verifyAndActivateFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public UserModel verifyAndActivate(String verificationToken) {
        String registrationId = jwtService.extractRegistrationIdFromVerificationToken(verificationToken);
        LogContext logContext = getLogContext("verifyAndActivate", Collections.emptyList());
        log.logInfo("Verifying registration id=" + registrationId, logContext);

        PendingUserRegistration pending = pendingUserRegistrationStore.findByRegistrationId(registrationId);
        if (pending == null) {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Registration not found or expired. Please register again.",
                Collections.singletonList(registrationId),
                "userModel"
            );
            log.logError(e.getMessage(), null, logContext);
            throw e;
        }

        List<Object> conflicts = new ArrayList<>();
        if (userRepository.existsByUsername(pending.getUsername())) {
            Map<String, Object> conflict = new HashMap<>();
            conflict.put("field", "username");
            conflict.put("value", pending.getUsername());
            conflict.put("message", "Username already exists");
            conflicts.add(conflict);
        }
        if (userRepository.existsByEmail(pending.getEmail())) {
            Map<String, Object> conflict = new HashMap<>();
            conflict.put("field", "email");
            conflict.put("value", pending.getEmail());
            conflict.put("message", "Email already exists");
            conflicts.add(conflict);
        }
        if (userRepository.existsByPhone(pending.getPhone())) {
            Map<String, Object> conflict = new HashMap<>();
            conflict.put("field", "phone");
            conflict.put("value", pending.getPhone());
            conflict.put("message", "Phone already exists");
            conflicts.add(conflict);
        }
        if (!conflicts.isEmpty()) {
            pendingUserRegistrationStore.delete(pending);
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Cannot activate registration due to duplicate fields",
                conflicts,
                "userModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(pending.getUsername());
        userEntity.setPassword(pending.getEncodedPassword());
        userEntity.setFullname(pending.getFullname());
        userEntity.setEmail(pending.getEmail());
        userEntity.setPhone(pending.getPhone());
        userEntity.setGender(pending.getGender());
        userEntity.setBirth(pending.getBirth());
        userEntity.setAddress(pending.getAddress());
        userEntity.setRole(pending.getRole());
        userEntity.setUserStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(userEntity);

        pendingUserRegistrationStore.delete(pending);
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, USER_REDIS_KEY_PREFIX);
        log.logInfo("User verified and activated successfully, id=" + userEntity.getId(), logContext);

        return toUserModel(userEntity);
    }

    // resend verification token
    @Override
    @CircuitBreaker(name = "user-service-register-verify", fallbackMethod = "resendVerificationTokenFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public void resendVerificationToken(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        LogContext logContext = getLogContext("resendVerificationToken", Collections.emptyList());
        log.logInfo("Resending verification token for email=" + normalizedEmail, logContext);

        String registrationId = pendingUserRegistrationStore.findRegistrationIdByEmail(normalizedEmail);
        if (registrationId == null) {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Pending registration not found or expired for email: " + normalizedEmail,
                Collections.singletonList(normalizedEmail),
                "userModel"
            );
            log.logError(e.getMessage(), null, logContext);
            throw e;
        }

        PendingUserRegistration pending = pendingUserRegistrationStore.findByRegistrationId(registrationId);
        if (pending == null) {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Pending registration not found or expired for email: " + normalizedEmail,
                Collections.singletonList(normalizedEmail),
                "userModel"
            );
            log.logError(e.getMessage(), null, logContext);
            throw e;
        }

        pendingUserRegistrationStore.refreshTtl(pending);
        log.logInfo("completed, resent verification email for registration: " + registrationId, logContext);
        emailService.sendVerificationEmail(pending.getEmail(), jwtService.generateVerificationToken(registrationId));
    }

    // ======================================== Helper Methods ========================================

    private UserModel toUserModel(UserEntity userEntity) {
        UserModel userModel = modelMapper.map(userEntity, UserModel.class);
        if (userEntity.getBirth() != null) {
            userModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
        }
        return userModel;
    }

    // thêm conflict vào list conflicts
    private void addRegistrationConflict(
        List<Object> conflicts, String field, String value,
        boolean existsInDb, boolean existsInRedis, String dbMessage,
        String pendingMessage
    ) {
        if (!existsInDb && !existsInRedis) {
            return;
        }
        Map<String, Object> conflict = new HashMap<>();
        conflict.put("field", field);
        conflict.put("value", value);
        conflict.put("message", existsInDb ? dbMessage : pendingMessage);
        conflicts.add(conflict);
    }

    private List<FilterCondition<UserEntity>> buildFilterConditions(
        Integer id, String username, String fullname, String email, String phone,
        Gender gender, LocalDate birth, String address, UserRole role, UserStatus userStatus
    ) {
        List<FilterCondition<UserEntity>> conditions = new ArrayList<>();
        if (id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if (StringUtils.hasText(username)) {
            conditions.add(FilterCondition.likeIgnoreCase("username", username));
        }
        if (StringUtils.hasText(fullname)) {
            conditions.add(FilterCondition.likeIgnoreCase("fullname", fullname));
        }
        if (StringUtils.hasText(email)) {
            conditions.add(FilterCondition.likeIgnoreCase("email", email));
        }
        if (StringUtils.hasText(phone)) {
            conditions.add(FilterCondition.likeIgnoreCase("phone", phone));
        }
        if (gender != null && (gender.equals(Gender.MALE) || gender.equals(Gender.FEMALE))) {
            conditions.add(FilterCondition.eq("gender", gender));
        }
        if (birth != null && birth.isBefore(LocalDate.now())) {
            conditions.add(FilterCondition.eq("birth", birth));
        }
        if (StringUtils.hasText(address)) {
            conditions.add(FilterCondition.likeIgnoreCase("address", address));
        }
        if (role != null && (role.equals(UserRole.ADMIN) ||
            role.equals(UserRole.CUSTOMER) || role.equals(UserRole.MANAGER) ||
            role.equals(UserRole.CASHIER))) {
            conditions.add(FilterCondition.eq("role", role));
        }
        if (userStatus != null) {
            conditions.add(FilterCondition.eq("userStatus", userStatus));
        }
        return conditions;
    }

    // ======================================== Fallback Methods ========================================

    @SuppressWarnings("unused")
    private Page<UserModel> filtersFallback(
        Integer id, String username, String fullname,
        String email, String phone, Gender gender,
        LocalDate birth, String address, UserRole role, UserStatus userStatus,
        Pageable pageable, Exception e
    ) {
        // là lỗi nghiệp vụ -> re-throw
        ResilienceFallbackUtils.rethrowBusinessThrowable(e);
        // nếu không phải lỗi circuit breaker open -> throw runtime exception
        if (!ResilienceFallbackUtils.isCircuitBreakerOpen(e)) {
            ResilienceFallbackUtils.throwAsRuntime(e);
        }

        // lấy thử data từ cache nếu có
        List<FilterCondition<UserEntity>> conditions = buildFilterConditions(
            id, username, fullname, email, phone, gender, birth, address, role, userStatus
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            USER_REDIS_KEY_PREFIX, conditions, pageable);
            
        Page<UserModel> cachedPage = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, UserModel.class);

        if (cachedPage != null && !cachedPage.isEmpty()) {
            log.logInfo(
                "Found cache when calling fallback filters method, returning...", 
                getLogContext("filters", Collections.emptyList())
            );
            return cachedPage;
        }

        // lỗi circuit breaker open -> throw service unavailable exception
        throw ResilienceFallbackUtils.serviceUnavailable("filters", e);
    }

    @SuppressWarnings("unused")
    private UserLoginModel loginFallback(LoginRequestModel req, Throwable e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "login");
        return null;
    }

    @SuppressWarnings("unused")
    private void logoutFallback(Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "logout");
    }

    @SuppressWarnings("unused")
    private List<UserRegisterModel> createsFallback(
        List<RegisterRequestModel> registers, Exception e
    ) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "creates");
        return null;
    }

    @SuppressWarnings("unused")
    private UserModel updateNormalFallback(
        UpdateUserNormalModel update, Integer userId, Exception e
    ) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updateNormal");
        return null;
    }

    @SuppressWarnings("unused")
    private List<UserModel> updatesForAdminFallback(
        List<UpdateUserForAdminModel> updates, List<Integer> userIds, Exception e
    ) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updatesForAdmin");
        return null;
    }

    @SuppressWarnings("unused")
    private UserModel updatePasswordByCustomerFallback(
        UserUpdatePasswordRequestModel update, Integer userId, Exception e
    ) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updatePasswordByCustomer");
        return null;
    }

    @SuppressWarnings("unused")
    private UserModel verifyAndActivateFallback(String verificationToken, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "verifyAndActivate");
        return null;
    }

    @SuppressWarnings("unused")
    private String resendVerificationTokenFallback(String email, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "resendVerificationToken");
        return null;
    }
}
