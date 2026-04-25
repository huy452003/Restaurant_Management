package com.common.utils;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;


// Vì PageImpl cần type nên phải có reader này để chuyển đổi data từ cache thành Page<T>

public final class RedisFilterPageCacheReader {

    private RedisFilterPageCacheReader() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Page<T> read(
        Object cachedData, Pageable pageable, 
        ObjectMapper objectMapper, Class<T> contentClass
    ) {
        if (cachedData == null) {
            return null;
        }
        if (cachedData instanceof Page<?>) {
            return (Page<T>) cachedData;
        }
        try {
            if (cachedData instanceof Map<?, ?> map) {
                // lấy contents từ Snapshot 
                Object contentObj = map.get("contents");
                if (contentObj == null) {
                    return null;
                }

                // lấy List<Type> từ contentClass
                JavaType listType = objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, contentClass);
                // chuyển đổi contents thành List<Type> đã lấy ở trên
                List<T> content = objectMapper.convertValue(contentObj, listType);

                long totalElements = 0L;
                Object totalObj = map.get("totalElements");
                // kiểm tra totalObj có phải là Number không sau đó ép về long cho page
                if (totalObj instanceof Number n) {
                    totalElements = n.longValue();
                }
                return new PageImpl<>(content, pageable, totalElements);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
