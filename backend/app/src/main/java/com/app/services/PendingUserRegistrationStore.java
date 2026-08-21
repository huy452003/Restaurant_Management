package com.app.services;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.common.models.user.PendingUserRegistration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.configurations.JwtConfig;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PendingUserRegistrationStore {

    private static final String DATA_PREFIX = "user:registration:data:";
    private static final String IDX_EMAIL = "user:registration:idx:email:";
    private static final String IDX_USERNAME = "user:registration:idx:username:";
    private static final String IDX_PHONE = "user:registration:idx:phone:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtConfig jwtConfig;

    private Duration ttl() {
        return Duration.ofMillis(jwtConfig.verificationExpiration());
    }

    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IDX_EMAIL + email));
    }

    public boolean existsByUsername(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IDX_USERNAME + username));
    }

    public boolean existsByPhone(String phone) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IDX_PHONE + phone));
    }

    public void save(PendingUserRegistration registration) {
        String registrationId = registration.getRegistrationId();
        Duration ttl = ttl();
        redisTemplate.opsForValue().set(DATA_PREFIX + registrationId, registration, ttl);
        redisTemplate.opsForValue().set(IDX_EMAIL + registration.getEmail(), registrationId, ttl);
        redisTemplate.opsForValue().set(IDX_USERNAME + registration.getUsername(), registrationId, ttl);
        redisTemplate.opsForValue().set(IDX_PHONE + registration.getPhone(), registrationId, ttl);
    }

    public PendingUserRegistration findByRegistrationId(String registrationId) {
        Object raw = redisTemplate.opsForValue().get(DATA_PREFIX + registrationId);
        if (raw == null) {
            return null;
        }
        if (raw instanceof PendingUserRegistration pending) {
            return pending;
        }
        return objectMapper.convertValue(raw, PendingUserRegistration.class);
    }

    public String findRegistrationIdByEmail(String email) {
        Object raw = redisTemplate.opsForValue().get(IDX_EMAIL + email);
        return raw != null ? raw.toString() : null;
    }

    public void delete(PendingUserRegistration registration) {
        if (registration == null) {
            return;
        }
        List<String> keys = new ArrayList<>();
        keys.add(DATA_PREFIX + registration.getRegistrationId());
        keys.add(IDX_EMAIL + registration.getEmail());
        keys.add(IDX_USERNAME + registration.getUsername());
        keys.add(IDX_PHONE + registration.getPhone());
        redisTemplate.delete(keys);
    }

    public void refreshTtl(PendingUserRegistration registration) {
        save(registration);
    }
}
