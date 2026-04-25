package com.common.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.models.cache.FilterFirstPageSnapshot;

public final class FilterFirstPageCacheUtils {

    public static final int DEFAULT_FIRST_PAGE_CACHE_TTL_SECONDS = 60;

    private FilterFirstPageCacheUtils() {
    }

    // kiểm tra xem đây có phải trang đầu tiên không để thao tác cache
    public static boolean shouldUseFirstPageSnapshotCache(Pageable pageable) {
        return pageable != null && pageable.getPageNumber() == 0;
    }

    // chuyển đổi page thành snapshot
    public static <T> FilterFirstPageSnapshot toSnapshot(Page<T> page) {
        return new FilterFirstPageSnapshot(page.getContent(), page.getTotalElements());
    }
}
