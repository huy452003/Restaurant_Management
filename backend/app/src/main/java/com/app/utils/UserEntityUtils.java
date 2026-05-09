package com.app.utils;

import java.util.Collections;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.common.entities.UserEntity;
import com.common.repositories.UserRepository;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.UnauthorizedExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

@Component
public class UserEntityUtils {

    @Autowired
    private UserRepository userRepository;

    // lấy user từ database by id 
    public UserEntity requireById(
        Integer userId, String modelName,
        LogContext logContext, LoggingService log
    ) {
        if(userId != null) {
            return userRepository.findById(userId).orElseThrow(() -> {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "User not found with id: " + userId,
                    Collections.singletonList(userId),
                    modelName
                );
                log.logError(e.getMessage(), e, logContext);
                return e;
            });
        } else {
            throw new IllegalArgumentException("userId cannot be null");
        }
    }

    // lấy user từ SecurityContextHolder (user đang đăng nhập)
    public UserEntity requireAuthenticatedUser(String modelName, LogContext logContext, LoggingService log) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : null;
        return userRepository.findByUsername(username).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "User not authenticated with username: " + username + " in this request",
                Collections.emptyList(),
                modelName
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    // kiểm tra user hiện tại có phải là user đang đăng nhập không ( qua id )
    public UserEntity requireAuthenticatedUserById(
        Integer userId, String modelName, LogContext logContext, LoggingService log
    ) {
        UserEntity currentUser = requireById(userId, modelName, logContext, log);
        UserEntity authenticatedUser = requireAuthenticatedUser(modelName, logContext, log);

        if (!Objects.equals(currentUser.getId(), authenticatedUser.getId())) {
            UnauthorizedExceptionHandle e = new UnauthorizedExceptionHandle(
                "User is not the same as the authenticated user",
                modelName
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        return currentUser;
    }
}
