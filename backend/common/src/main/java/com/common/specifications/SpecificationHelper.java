package com.common.specifications;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

public final class SpecificationHelper {
    private SpecificationHelper() {
    }

    public static <T> Specification<T> buildSpecification(List<FilterCondition<T>> conditions) {
        Specification<T> result = null;
        for (FilterCondition<T> condition : conditions) {
            Specification<T> conditionSpec = condition.toSpecification(); // biến condition thành Specification
            result = (result == null) ? conditionSpec : result.and(conditionSpec);
        }
        return result != null ? result : (root, query, criteriaBuilder) -> criteriaBuilder.conjunction(); 
    }
}
