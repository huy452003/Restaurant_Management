package com.common.models.cache;

import java.util.Collections;
import java.util.List;

public record FilterFirstPageSnapshot(List<?> contents, long totalElements) {

    public FilterFirstPageSnapshot {
        contents = contents != null ? contents : Collections.emptyList();
    }
}
