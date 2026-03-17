package com.app.services.imp;

import com.app.services.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import com.common.models.user.LoginModel;
import com.common.models.user.RegisterModel;
import com.common.models.user.UpdateUserForAdminModel;
import com.common.models.user.UpdateUserNormalModel;
import com.common.models.user.UserModel;
import com.common.models.user.UserSecurityModel;
import com.common.repositories.UserRepository;
import com.common.enums.UserRole;
import com.common.enums.UserStatus;
import com.common.entities.UserEntity;
import com.common.utils.AgeUtils;
import com.common.enums.Gender;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ServiceUnavailableExceptionHandle;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.ForbiddenExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.handle_exceptions.UnauthorizedExceptionHandle;
import com.security.configurations.JwtConfig;
import com.security.services.BlackListService;
import com.security.services.JwtService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class UserServiceImp implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JwtConfig jwtConfig;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private BlackListService blackListService;

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

    // get all users
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "getAllFallback")
    public List<UserModel> getAll() {
        LogContext logContext = getLogContext("getAll", Collections.emptyList());
        log.logInfo("Getting all users ...!", logContext);

        // lấy cache từ redis nếu có
        String redisKeyGetAllUsers = USER_REDIS_KEY_PREFIX + "getAllUsers";
        List<UserModel> cachedUsers = (List<UserModel>) redisTemplate.opsForValue().get(redisKeyGetAllUsers);
        if(cachedUsers != null && !cachedUsers.isEmpty()) {
            log.logInfo("completed, found " + cachedUsers.size() + " users in cache", logContext);
            return cachedUsers;
        }
        log.logInfo("Not found users in cache, query from database", logContext);

        // query từ db
        List<UserEntity> userEntities = userRepository.findAll();
        if(userEntities == null || userEntities.isEmpty()){
            NotFoundExceptionHandle e = new NotFoundExceptionHandle("No users found", Collections.emptyList(), "userModel");
            log.logError("threw an exception", e, logContext);
            throw e;
        }
        
        // Map từ Entity sang Model trước khi lưu cache
        List<UserModel> userModels = userEntities.stream().map(
            userEntity -> {
                UserModel userModel = modelMapper.map(userEntity, UserModel.class);
                if(userEntity.getBirth() != null){
                    userModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
                }
                return userModel;
            }
        ).collect(Collectors.toList());
        
        // lưu cache vào redis
        redisTemplate.opsForValue().set(redisKeyGetAllUsers, userModels);
        log.logInfo("completed, found " + userModels.size() + " users", logContext);
        log.logInfo("cached " + userModels.size() + " users in key: " + redisKeyGetAllUsers, logContext);
        return userModels;
    }

    // login
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "loginFallback")
    public UserSecurityModel login(LoginModel req) {
        LogContext logContext = getLogContext("login", Collections.emptyList());
        log.logInfo("Logging in user ...!", logContext);

        // xác thực username và password
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );

        // query từ db
        UserEntity userEntity = userRepository.findByUsername(req.getUsername()).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "User not found with username: " + req.getUsername() + "when login",
                Collections.emptyList(),
                "UserSecurityModel"
            );
            log.logError(e.getMessage(), null, logContext);
            return e;
        });

        // kiểm tra user status phải là ACTIVE
        if(userEntity.getUserStatus() == UserStatus.INACTIVE){
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "User is inactive",
                "UserSecurityModel",
                "userStatus must be ACTIVE"
            );
            log.logError(e.getMessage(), null, logContext);
            throw e;
        }
        if(userEntity.getUserStatus() == UserStatus.PENDING){
            ForbiddenExceptionHandle e = new ForbiddenExceptionHandle(
                "User is pending",
                "UserSecurityModel",
                "userStatus must be ACTIVE"
            );
            log.logError(e.getMessage(), null, logContext);
            throw e;
        }
        
        // trả response với tokens
        UserSecurityModel userSecurityModel = modelMapper.map(userEntity, UserSecurityModel.class);
        Map<String, Object> claim = new HashMap<>();
        claim.put("role", userEntity.getRole().name());
        userSecurityModel.setAccessToken(jwtService.generateAccessToken(claim, userEntity));
        userSecurityModel.setExpires(formatExpirationTime(jwtConfig.expiration()));
        userSecurityModel.setRefreshToken(jwtService.generateRefreshToken(claim, userEntity));
        userSecurityModel.setRefreshExpires(formatExpirationTime(jwtConfig.refreshExpiration()));
        if(userEntity.getBirth() != null){
            userSecurityModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
        }
        log.logInfo("completed, logged in user: " + userEntity.getUsername(), logContext);
        return userSecurityModel;
    }

    // logout
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "logoutFallback")
    public void logout(String username) {
        LogContext logContext = getLogContext("logout", Collections.emptyList());
        log.logInfo("User is logging out ...!", logContext);

        // kiểm tra username có tồn tại trong db không
        UserEntity foundUser = userRepository.findByUsername(username).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Not found user with username: " + username,
                 Collections.emptyList(),
                 "userModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });

        // thêm token vào blacklist (revoke token)
        blackListService.blackListUser(foundUser.getUsername());
        log.logInfo("Completed, logged out user: " + foundUser.getUsername() + " and blacklisted token", logContext);
    }

    // create users
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "createsFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public List<UserSecurityModel> creates(List<RegisterModel> registers) {
        LogContext logContext = getLogContext("creates", Collections.emptyList());
        log.logInfo("Creating " + registers.size() + " users ...!", logContext);

        // Check nếu có user muốn tạo ADMIN role nhưng đã có ADMIN trong hệ thống
        boolean hasAdminInSystem = userRepository.existsByRole(UserRole.ADMIN);
        for (RegisterModel register : registers) {
            if (register.getRole().equals(UserRole.ADMIN) && hasAdminInSystem) {
                ValidationExceptionHandle e = new ValidationExceptionHandle(
                    "Admin role already exists",
                    Collections.singletonList(register.getRole()),
                    "UserSecurityModel"
                );
                log.logError("System already has an admin user, cannot create another admin", e, logContext);
                throw e;
            }
        }

        // Kiểm tra trùng lặp trước khi lưu (normalize input để so sánh với data đã normalize trong DB)
        List<Object> conflicts = new ArrayList<>();
        for (RegisterModel register : registers) {
            // Normalize input
            register.setUsername(register.getUsername().toLowerCase().trim());
            register.setEmail(register.getEmail().toLowerCase().trim());
            
            if (userRepository.existsByUsername(register.getUsername())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "username");
                conflict.put("value", register.getUsername());
                conflict.put("message", "Username already exists");
                conflicts.add(conflict);
            }
            if (userRepository.existsByEmail(register.getEmail())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "email");
                conflict.put("value", register.getEmail());
                conflict.put("message", "Email already exists");
                conflicts.add(conflict);
            }
            if (userRepository.existsByPhone(register.getPhone())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "phone");
                conflict.put("value", register.getPhone());
                conflict.put("message", "Phone already exists");
                conflicts.add(conflict);
            }
        }
        
        if (!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "UserSecurityModel"
            );
            log.logError("Thrown an exception when create users", e, logContext);
            throw e;
        }

        // chuyển đổi từ models sang entities để save vào db
        List<UserEntity> userEntities = registers.stream().map(register -> {
            UserEntity userEntity = modelMapper.map(register, UserEntity.class);
            userEntity.setPassword(passwordEncoder.encode(register.getPassword()));
            if (userEntity.getUserStatus() == null) {
                userEntity.setUserStatus(UserStatus.PENDING);
            }
            return userEntity;
        }).collect(Collectors.toList());
    
        userRepository.saveAll(userEntities);   

        // xóa cache
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "getAllUsers");
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "filters");
        log.logInfo("Deleted cache for getAllUsers and filters", logContext);
        log.logInfo("completed, created " + userEntities.size() + " users with PENDING status", logContext);
        return userEntities.stream().map(userEntity -> {
            UserSecurityModel userSecurityModel = modelMapper.map(userEntity, UserSecurityModel.class);
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", userEntity.getRole().name());
            userSecurityModel.setAccessToken(jwtService.generateAccessToken(claims, userEntity));
            userSecurityModel.setExpires(formatExpirationTime(jwtConfig.expiration()));
            userSecurityModel.setRefreshToken(jwtService.generateRefreshToken(claims, userEntity));
            userSecurityModel.setRefreshExpires(formatExpirationTime(jwtConfig.refreshExpiration()));
            userSecurityModel.setVerificationToken(jwtService.generateVeruficationToken(userEntity.getId()));
            userSecurityModel.setVerificationTokenExpires(formatExpirationTime(86400000L));
            if (userEntity.getBirth() != null) {
                userSecurityModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
            }
            return userSecurityModel;
        }).collect(Collectors.toList());
    }

    // update user normal - user tự update thông tin của chính mình
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "updateNormalFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public UserModel updateNormal(UpdateUserNormalModel update, Integer userId) {
        LogContext logContext = getLogContext(
            "updateNormal", 
            Collections.singletonList(userId)
        );       
        log.logInfo("User with id " + userId + " is updating their profile ...!", logContext);

        // Tìm user từ userId
        UserEntity currentUser = userRepository.findById(userId).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "User not found with id: " + userId,
                Collections.singletonList(userId),
                "userModel"
            );
            log.logError("User not found", e, logContext);
            return e;
        });

        // Normalize email
        update.setEmail(update.getEmail().toLowerCase().trim());
        
        // Kiểm tra trùng lặp trước khi cập nhật
        List<Object> conflicts = new ArrayList<>();
        
        // Check email: CHỈ check duplicate khi email THAY ĐỔI (khác với email hiện tại)
        if (!update.getEmail().equals(currentUser.getEmail())) {
            if (userRepository.existsByEmail(update.getEmail())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "email");
                conflict.put("value", update.getEmail());
                conflict.put("message", "Email already exists for another user");
                conflicts.add(conflict);
            }
        }
        
        // Check phone: CHỈ check duplicate khi phone THAY ĐỔI (khác với phone hiện tại)
        if (!update.getPhone().equals(currentUser.getPhone())) {
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
        boolean hasChanges = !update.getFullname().equals(currentUser.getFullname()) ||
                             !update.getEmail().equals(currentUser.getEmail()) ||
                             !update.getPhone().equals(currentUser.getPhone()) ||
                             !update.getGender().equals(currentUser.getGender()) ||
                             !update.getBirth().equals(currentUser.getBirth()) ||
                             !update.getAddress().equals(currentUser.getAddress());
        
        if (hasChanges) {
            // Map các field từ update model vào UserEntity
            modelMapper.map(update, currentUser);
            userRepository.save(currentUser);
            log.logInfo("completed, updated user with id: " + userId, logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        // xóa cache
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "getAllUsers");
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "filters");
        log.logInfo("Deleted cache for getAllUsers and filters", logContext);
        
        // Return user đã update
        UserModel userModel = modelMapper.map(currentUser, UserModel.class);
        if (currentUser.getBirth() != null) {
            userModel.setAge(AgeUtils.calculateAge(currentUser.getBirth()));
        }
        return userModel;
    }

    // update user for admin
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "updatesForAdminFallback")
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
        List<UserEntity> foundUsers = userIds.stream().map(id -> userRepository.findById(id).orElseThrow(() -> {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "User not found with id: " + id,
                    Collections.singletonList(id),
                    "UserModel"
                );
                log.logError(e.getMessage(), e, logContext);
                return e;
            })
        ).collect(Collectors.toList());

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

            // Normalize username và email
            update.setUsername(update.getUsername().toLowerCase().trim());
            update.setEmail(update.getEmail().toLowerCase().trim());

            if(!update.getUsername().equals(currentUser.getUsername())){
                if(userRepository.existsByUsername(update.getUsername())){
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "username");
                    conflict.put("value", update.getUsername());
                    conflict.put("message", "Username already exists for another user");
                    conflicts.add(conflict);
                }
            }
            if(!update.getEmail().equals(currentUser.getEmail())){
                if(userRepository.existsByEmail(update.getEmail())){
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "email");
                    conflict.put("value", update.getEmail());
                    conflict.put("message", "Email already exists for another user");
                    conflicts.add(conflict);
                }
            }
            if(!update.getPhone().equals(currentUser.getPhone())){
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

            boolean hasChanges = !update.getUsername().equals(currentUser.getUsername()) ||
                                 !update.getFullname().equals(currentUser.getFullname()) ||
                                 !update.getEmail().equals(currentUser.getEmail()) ||
                                 !update.getPhone().equals(currentUser.getPhone()) ||
                                 !update.getGender().equals(currentUser.getGender()) ||
                                 !update.getBirth().equals(currentUser.getBirth()) ||
                                 !update.getAddress().equals(currentUser.getAddress()) ||
                                 !update.getRole().equals(currentUser.getRole()) ||
                                 !update.getUserStatus().equals(currentUser.getUserStatus());

            if(hasChanges){
                modelMapper.map(update, currentUser);
                usersToUpdate.add(currentUser);
            }
        }

        if(!usersToUpdate.isEmpty()){
            userRepository.saveAll(usersToUpdate);
            log.logInfo("completed, updated " + usersToUpdate.size() + " users by admin", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        // xóa cache
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "getAllUsers");
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "filters");
        log.logInfo("Deleted cache for getAllUsers and filters", logContext);
        // Return tất cả users (bao gồm cả những user không có thay đổi)
        return foundUsers.stream().map(userEntity -> {
                UserModel userModel = modelMapper.map(userEntity, UserModel.class);
                if(userEntity.getBirth() != null){
                    userModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
                }
                return userModel;
            }
        ).collect(Collectors.toList());
    }

    // filter users with pagination
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "filtersFallback")
    public Page<UserModel> filters(
        Integer id, String username, String fullname,
        String email, String phone, Gender gender,
        LocalDate birth, String address, UserRole role, UserStatus userStatus,
        Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering users with pagination ...!", logContext);

        // lấy cache từ redis nếu có
        String redisKeyFilters = USER_REDIS_KEY_PREFIX + "filters";
        Page<UserModel> cachedPage = (Page<UserModel>) redisTemplate.opsForValue().get(redisKeyFilters);
        if(cachedPage != null && !cachedPage.isEmpty()){
            log.logInfo("completed, found " + cachedPage.getTotalElements() + " users in cache", logContext);
            return cachedPage;
        }
        log.logInfo("not found users in cache, query from database", logContext);

        // tạo map conditions để query theo các điều kiện hợp lệ
        Map<String, Object> conditions = new HashMap<>();

        if(id != null && id > 0){
            conditions.put("id", id);
        }
        if(username != null){
            conditions.put("username", username);
        }
        if(fullname != null){
            conditions.put("fullname", fullname);
        }
        if(email != null){
            conditions.put("email", email);
        }
        if(phone != null){
            conditions.put("phone", phone);
        }
        if(gender != null && (gender.equals(Gender.MALE) || gender.equals(Gender.FEMALE))){
            conditions.put("gender", gender);
        }
        if(birth != null && birth.isBefore(LocalDate.now())){
            conditions.put("birth", birth);
        }
        if(address != null){
            conditions.put("address", address);
        }
        if(role != null && (role.equals(UserRole.ADMIN) ||
           role.equals(UserRole.CUSTOMER) || role.equals(UserRole.MANAGER) || 
           role.equals(UserRole.WAITER) || role.equals(UserRole.CHEF) || 
           role.equals(UserRole.CASHIER))){
            conditions.put("role", role);
        }
        if(userStatus != null && userStatus.equals(UserStatus.ACTIVE)){
            conditions.put("userStatus", userStatus);
        }

        if(conditions.isEmpty()){
            Page<UserEntity> pageEntites = userRepository.findAll(pageable);
            List<UserModel> userModels = pageEntites.getContent().stream().map(userEntity -> {
                UserModel userModel = modelMapper.map(userEntity, UserModel.class);
                if(userEntity.getBirth() != null){
                    userModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
                }
                return userModel;
            }).collect(Collectors.toList());
            log.logWarn("No conditions provided, returning all users with pagination", logContext);
            return new PageImpl<>(userModels, pageEntites.getPageable(), pageEntites.getTotalElements());
        }
        log.logInfo("Filtering users with conditions: " + conditions, logContext);

        // Build specification từ map conditions
        Specification<UserEntity> spec = buildSpecificationFromConditions(conditions);
        
        // Query với pagination
        Page<UserEntity> pageEntities = userRepository.findAll(spec, pageable);
        
        if(pageEntities.isEmpty()){
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "No users found with the given conditions or pagination",
                Collections.emptyList(),
                "UserModel"
            );
            log.logError("No users found with conditions or pagination", e, logContext);
            throw e;
        }
        
        // Map từ Entity sang Model và tính age
        List<UserModel> pageDatas = pageEntities.getContent().stream().map(userEntity -> {
            UserModel userModel = modelMapper.map(userEntity, UserModel.class);
            if(userEntity.getBirth() != null){
                userModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
            }
            return userModel;
        }).collect(Collectors.toList());
        
        // Tạo Page<UserModel> từ List<UserModel> và thông tin pagination từ Page<UserEntity>
        Page<UserModel> userModelPage = new PageImpl<>(
            pageDatas,                      // data 
            pageEntities.getPageable(),     // pageable (để lấy số trang hiện tại, số phần tử mỗi trang)
            pageEntities.getTotalElements() // total elements (để lấy tổng số phần tử)
        );
        // lưu cache vào redis
        redisTemplate.opsForValue().set(redisKeyFilters, userModelPage);
        log.logInfo("cached " + userModelPage.getTotalElements() + " users in key: " + redisKeyFilters, logContext);
        log.logInfo("completed, found " + userModelPage.getTotalElements() + " users in " + 
                    userModelPage.getTotalPages() + " pages", logContext);
        return userModelPage;
    }

    // verify and activate user
    @Override
    @CircuitBreaker(name = "restaurant-management-service", fallbackMethod = "verifyAndActivateFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public UserModel verifyAndActivate(String verificationToken) {
        Integer userId = jwtService.extractUserIdFromVerificationToken(verificationToken);
        LogContext logContext = getLogContext(
            "verifyAndActivate", 
            Collections.singletonList(userId)
        );
        log.logInfo("Verifying user with verification token ...!", logContext);

        UserEntity userEntity = userRepository.findById(userId)
            .orElseThrow(() -> {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "User not found with id: " + userId,
                    Collections.singletonList(userId),
                    "userModel"
                );
                log.logError("User not found", e, logContext);
                return e;
            });

        // Kiểm tra status phải là PENDING
        if (userEntity.getUserStatus() != UserStatus.PENDING) {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "User is not in PENDING status. Current status: " + userEntity.getUserStatus(),
                Collections.singletonList(userId),
                "userModel"
            );
            log.logError("User status is not PENDING", e, logContext);
            throw e;
        }

        // Activate user sau khi verify thành công
        userEntity.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(userEntity);

        // xóa cache
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "getAllUsers");
        redisTemplate.delete(USER_REDIS_KEY_PREFIX + "filters");
        log.logInfo("Deleted cache for getAllUsers and filters when verify and activate user", logContext);
        
        log.logInfo("User verified and activated successfully", logContext);
        
        UserModel userModel = modelMapper.map(userEntity, UserModel.class);
        if (userEntity.getBirth() != null) {
            userModel.setAge(AgeUtils.calculateAge(userEntity.getBirth()));
        }
        return userModel;
    }

    // build specification từ map conditions để có thể query theo các điều kiện hợp lệ
    private Specification<UserEntity> buildSpecificationFromConditions(Map<String, Object> conditions) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            conditions.forEach((field, value) -> {
                if (value == null) {
                    return;
                }

                switch (field) {
                    case "id":
                        predicates.add(criteriaBuilder.equal(root.get("id"), value));
                        break;
                    case "username":
                    case "fullname":
                    case "email":
                    case "phone":
                    case "address":
                        String searchValue = "%" + ((String) value).toLowerCase().trim() + "%";
                        predicates.add(criteriaBuilder.like( criteriaBuilder.lower(root.get(field)),searchValue));
                        break;
                    case "gender":
                        predicates.add(criteriaBuilder.equal(root.get("gender"), value));
                        break;
                    case "birth":
                        predicates.add(criteriaBuilder.equal(root.get("birth"), value));
                        break;
                    case "role":
                        predicates.add(criteriaBuilder.equal(root.get("role"), value));
                        break;
                    case "userStatus":
                        predicates.add(criteriaBuilder.equal(root.get("userStatus"), value));
                        break;
                }
            });
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    //Fallback method *************************************************************//

    // getAllFallback
    @SuppressWarnings("unused")
    private List<UserModel> getAllFallback(Exception e) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Thử lấy từ cache trước khi throw ServiceUnavailableExceptionHandle
        String redisKeyGetAllUsers = USER_REDIS_KEY_PREFIX + "getAllUsers";
        List<UserModel> cachedUsers = (List<UserModel>) redisTemplate.opsForValue().get(redisKeyGetAllUsers);
        if(cachedUsers != null && !cachedUsers.isEmpty()) {
            return cachedUsers;
        }
        
        // Service unavailable và không có cache
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for getAll service", 
            "getAll"
        );
        throw error;
    }

    // loginFallback
    @SuppressWarnings("unused")
    private UserSecurityModel loginFallback(
        LoginModel req, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Service unavailable hoặc exception khác
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for login service", 
            "login"
        );
        throw error;
    }

    // logoutFallback
    @SuppressWarnings("unused")
    private void logoutFallback(
        String username, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Service unavailable hoặc exception khác
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for logout service", 
            "logout"
        );
        throw error;
    }

    // createsFallback
    @SuppressWarnings("unused")
    private List<UserSecurityModel> createsFallback(
        List<RegisterModel> registers, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Service unavailable hoặc exception khác
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for creates service", 
            "creates"
        );
        throw error;
    }

    // updateNormalFallback
    @SuppressWarnings("unused")
    private UserModel updateNormalFallback(
        UpdateUserNormalModel update, Integer userId, Exception e
    ) {
        // Nếu là business exception (ConflictExceptionHandle, NotFoundExceptionHandle, etc.)
        // thì re-throw để exception handler xử lý đúng (409, 404, etc.)
        // Chỉ throw ServiceUnavailableExceptionHandle khi thực sự service unavailable
        if (e instanceof ConflictExceptionHandle || 
            e instanceof NotFoundExceptionHandle ||
            e instanceof ValidationExceptionHandle ||
            e instanceof ForbiddenExceptionHandle ||
            e instanceof UnauthorizedExceptionHandle) {
            throw (RuntimeException) e;
        }
        
        // Service unavailable hoặc exception khác
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for updateNormal service", 
            "updateNormal"
        );
        // Không dùng log trong fallback vì private method không được inject dependency
        throw error;
    }

    // updatesForAdminFallback
    @SuppressWarnings("unused")
    private List<UserModel> updatesForAdminFallback(
        List<UpdateUserForAdminModel> updates, List<Integer> userIds, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Service unavailable hoặc exception khác
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for updatesForAdmin service", 
            "updatesForAdmin"
        );
        throw error;
    }

    // filtersFallback
    @SuppressWarnings("unused")
    private Page<UserModel> filtersFallback(
        Integer id, String username, String fullname,
        String email, String phone, Gender gender,
        LocalDate birth, String address, UserRole role, UserStatus userStatus,
        Pageable pageable, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Thử lấy từ cache trước khi throw ServiceUnavailableExceptionHandle
        String redisKeyFilters = USER_REDIS_KEY_PREFIX + "filters";
        Page<UserModel> cachedPage = (Page<UserModel>) redisTemplate.opsForValue().get(redisKeyFilters);
        if(cachedPage != null && !cachedPage.isEmpty()) {
            return cachedPage;
        }
        
        // Service unavailable và không có cache
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for filters service", 
            "filters"
        );
        throw error;
    }

    // verifyAndActivateFallback
    @SuppressWarnings("unused")
    private UserModel verifyAndActivateFallback(
        String verificationToken, Exception e
    ) {
        // Re-throw business exceptions để exception handler xử lý đúng
        if (isBusinessException(e)) {
            throw (RuntimeException) e;
        }
        
        // Service unavailable hoặc exception khác
        ServiceUnavailableExceptionHandle error = new ServiceUnavailableExceptionHandle(
            e != null && e.getMessage() != null ? e.getMessage() : "Circuit breaker for verifyAndActivate service", 
            "verifyAndActivate"
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
