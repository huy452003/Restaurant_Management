package com.security.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Collections;

@Service
public class BlackListService {
    @Autowired
    @Qualifier("customStringRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private LoggingService log;

    private static final String BLACK_LIST_PREFIX = "black_list:";

    private LogContext getLogContext(String methodName, List<Integer> userIds) {
        return LogContext.builder()
            .module("security")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(userIds)
            .build();
    }

    // black list user
    public void blackListUser(String username) {
        LogContext logContext = getLogContext("blackListUser", Collections.emptyList());
        try {
            // lưu token vào redis để blacklist token đó
            long logoutTime = System.currentTimeMillis();
            redisTemplate.opsForValue().set(
                BLACK_LIST_PREFIX + "user: " + username,
                String.valueOf(logoutTime),
                Duration.ofDays(7)
            );

            String readableTime = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

            log.logInfo("User " + username + " has been blacklisted until " + readableTime + ".", logContext);
        } catch (Exception e) {
            log.logError("Error blacklisting user " + username, e, logContext);
            throw new RuntimeException("Error blacklisting user " + username, e);
        }
    }

    // kiểm tra token có nằm trong blacklist không
    public boolean isBlackListed(String token, String username) {
        LogContext logContext = getLogContext("isBlackListed", Collections.emptyList());
        try {
            String userBlackListKey = BLACK_LIST_PREFIX + "user: " + username;
            String logoutTime = redisTemplate.opsForValue().get(userBlackListKey);

            if(logoutTime != null) {
                Long logoutTimestamp = Long.parseLong(logoutTime);
                Date tokenIssuedAt = jwtService.extractIssuedAt(token);

                // Nếu token được tạo trước thời điểm logout toàn bộ, nó bị blacklist
                return tokenIssuedAt != null && tokenIssuedAt.getTime() < logoutTimestamp;
            }
            return false;
        } catch (Exception e) {
            log.logError("Error checking if user " + username + " is blacklisted", e, logContext);

            return false;
        }
    }

}
