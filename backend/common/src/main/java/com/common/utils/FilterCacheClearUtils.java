package com.common.utils;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;

public final class FilterCacheClearUtils {
    private FilterCacheClearUtils() {
    }

    public static void clear(RedisTemplate<String, Object> redisTemplate, String keyPrefix) {
        Set<String> keys = redisTemplate.keys(keyPrefix + "filters:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
