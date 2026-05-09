package com.common.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;

import com.common.specifications.FilterCondition;
import com.common.specifications.FilterMatchType;

public final class FilterCacheKeyUtils {
    private FilterCacheKeyUtils() {
    }

    // tạo key 
    public static <T> String build(
        String keyPrefix, List<FilterCondition<T>> conditions, Pageable pageable
    ) {
        StringBuilder keyBuilder = new StringBuilder(keyPrefix).append("filters:");
        conditions.stream()
            .sorted(Comparator.comparing(FilterCondition::getField))
            .forEach(condition -> keyBuilder
                .append(condition.getField())
                .append("=")
                .append(normalizeFilterValue(condition.getValue(), condition.getMatchType()))
                .append("&"));

        keyBuilder.append("page=").append(pageable.getPageNumber())
            .append("&size=").append(pageable.getPageSize())
            .append("&sort=").append(pageable.getSort().toString().replace(" ", ""));
        return keyBuilder.toString();
    }

    // xóa cache
    public static void clear(RedisTemplate<String, Object> redisTemplate, String keyPrefix) {
        Set<String> keys = redisTemplate.keys(keyPrefix + "filters:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private static String normalizeFilterValue(Object value, FilterMatchType matchType) {
        if (value == null) {
            return "null";
        }
        if (FilterMatchType.LIKE_IGNORE_CASE.equals(matchType) && value instanceof String) {
            return String.valueOf(value).trim().toLowerCase();
        }
        return String.valueOf(value).trim();
    }
}
