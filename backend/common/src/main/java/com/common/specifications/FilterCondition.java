package com.common.specifications;

import org.springframework.data.jpa.domain.Specification;

import lombok.Data;

@Data
public class FilterCondition<T> {
    private final String field;
    private final Object value;
    private final FilterMatchType matchType;

    private FilterCondition(String field, Object value, FilterMatchType matchType) {
        this.field = field;
        this.value = value;
        this.matchType = matchType;
    }

    public static <T> FilterCondition<T> eq(String field, Object value) {
        return new FilterCondition<>(field, value, FilterMatchType.EQ);
    }

    public static <T> FilterCondition<T> likeIgnoreCase(String field, String value) {
        return new FilterCondition<>(field, value, FilterMatchType.LIKE_IGNORE_CASE);
    }

    public Specification<T> toSpecification() {
        return (root, query, criteriaBuilder) -> {
            if (matchType == FilterMatchType.LIKE_IGNORE_CASE) {
                String searchValue = "%" + String.valueOf(value).toLowerCase().trim() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get(field)), searchValue);
            }
            return criteriaBuilder.equal(root.get(field), value);
        };
    }
}
