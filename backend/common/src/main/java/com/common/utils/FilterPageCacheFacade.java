package com.common.utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;

import com.common.specifications.FilterCondition;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class FilterPageCacheFacade {

    private FilterPageCacheFacade() {
    }

    public static <E> String buildFirstPageKeyIfApplicable(
        String keyPrefix, List<FilterCondition<E>> conditions, Pageable pageable
    ) {
        // kiểm tra xem đây có phải trang đầu tiên không để thao tác cache
        if (!FilterFirstPageCacheUtils.shouldUseFirstPageSnapshotCache(pageable)) {
            return null;
        }
        // tạo key cache
        return FilterCacheKeyUtils.build(keyPrefix, conditions, pageable);
    }

    public static void clearFirstPageCache(RedisTemplate<String, Object> redisTemplate, String keyPrefix) {
        FilterCacheKeyUtils.clear(redisTemplate, keyPrefix);
    }

    public static <T> Page<T> readFirstPageCache(
        RedisTemplate<String, Object> redisTemplate, String cacheKey,
        Pageable pageable, ObjectMapper objectMapper, Class<T> contentClass
    ) {
        if (cacheKey == null) {
            return null;
        }
        // lấy data từ cache key
        Object cachedData = redisTemplate.opsForValue().get(cacheKey);
        // chuyển đổi data từ cache thành Page<T>
        Page<T> cachedPage = RedisFilterPageCacheReader.read(cachedData, pageable, objectMapper, contentClass);
        if (cachedData != null && cachedPage == null && !(cachedData instanceof Page<?>)) {
            log.warn("Cached data is not a page, deleting cache key: " + cacheKey);
            redisTemplate.delete(cacheKey);
        }
        return cachedPage;
    }

    public static <T> void writeFirstPageCache(
        RedisTemplate<String, Object> redisTemplate,
        String cacheKey,
        Page<T> page
    ) {
        if (cacheKey == null || page == null) {
            return;
        }
        redisTemplate.opsForValue().set(
            cacheKey,
            FilterFirstPageCacheUtils.toSnapshot(page),
            FilterFirstPageCacheUtils.DEFAULT_FIRST_PAGE_CACHE_TTL_SECONDS,
            TimeUnit.SECONDS
        );
    }
}
