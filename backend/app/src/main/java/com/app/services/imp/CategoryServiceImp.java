package com.app.services.imp;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.app.services.CategoryService;
import com.common.repositories.CategoryRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.common.models.category.CategoryModel;
import com.common.entities.CategoryEntity;
import com.common.enums.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Retryable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.handle_exceptions.support.ResilienceFallbackUtils;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.modelmapper.ModelMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImp implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LoggingService log;
    private final ModelMapper modelMapper;

    private LogContext getLogContext(String methodName, List<Integer> categoryIds) {
        return LogContext.builder()
            .module("app")
            .className(this.getClass().getSimpleName())
            .methodName(methodName)
            .ids(categoryIds)
            .build();
    }

    private static final String CATEGORY_REDIS_KEY_PREFIX = "category:";

    @Override
    @CircuitBreaker(name = "category-service-read", fallbackMethod = "filtersFallback")
    public Page<CategoryModel> filters(
        Integer id, String name, CategoryStatus categoryStatus, Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering categories with pagination ...!", logContext);

        List<FilterCondition<CategoryEntity>> conditions = buildFilterConditions(
            id, name, categoryStatus
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            CATEGORY_REDIS_KEY_PREFIX, conditions, pageable
        );

        Page<CategoryModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, CategoryModel.class
        );

        if(cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " categories in cache", logContext);
            return cached;
        }

        Page<CategoryEntity> pageEntities;
        if(conditions.isEmpty()) {
            pageEntities = categoryRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all categories with pagination", logContext);
        }else {
            Specification<CategoryEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = categoryRepository.findAll(spec, pageable);
        }

        List<CategoryModel> pageDatas = pageEntities.getContent().stream().map(
            categoryEntity -> modelMapper.map(categoryEntity, CategoryModel.class)
        ).collect(Collectors.toList());

        Page<CategoryModel> categoryModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if(redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, categoryModelPage);
            log.logInfo("cached first-page filter snapshot for " + categoryModelPage.getTotalElements()
                + " categories, key: " + redisKeyFilters, logContext);
        }
        return categoryModelPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @CircuitBreaker(name = "category-service-write", fallbackMethod = "createsFallback")
    public List<CategoryModel> creates(List<CategoryModel> categories) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating categories ...!", logContext);

        List<Object> conflicts = new ArrayList<>();
        for(CategoryModel cate : categories) {
            if(categoryRepository.existsByName(cate.getName())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "name");
                conflict.put("value", cate.getName());
                conflict.put("message", "Name already exists");
                conflicts.add(conflict);
            }
        }

        if(!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "CategoryModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<CategoryEntity> categoryEntities = categories.stream().map(
            cateroryModel -> modelMapper.map(cateroryModel, CategoryEntity.class)
        ).collect(Collectors.toList());

        categoryRepository.saveAll(categoryEntities);

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, CATEGORY_REDIS_KEY_PREFIX);

        log.logInfo("Deleted filter caches after create", logContext);
        
        log.logInfo("completed, created " + categoryEntities.size() + " categories", logContext);
        return categoryEntities.stream().map(
            entity -> modelMapper.map(entity, CategoryModel.class)
        ).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @CircuitBreaker(name = "category-service-write", fallbackMethod = "updatesFallback")
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public List<CategoryModel> updates(List<CategoryModel> updates, List<Integer> categoryIds) {
        LogContext logContext = getLogContext("update", categoryIds);
        log.logInfo("Updating categories ...!", logContext);

        if(updates.size() != categoryIds.size()){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and categoryIds",
                categoryIds,
                "CategoryModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<CategoryEntity> foundCate = categoryIds.stream().map(
            id -> categoryRepository.findById(id).orElseThrow(() -> {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "Category not found with id: " + id,
                    Collections.singletonList(id),
                    "CategoryModel"
                );
                log.logError(e.getMessage(), e, logContext);
                return e;
            })
        ).collect(Collectors.toList());
        log.logInfo("found " + foundCate.size() + " categories", logContext);

        List<Object> conflicts = new ArrayList<>();
        for(int i = 0; i < updates.size(); i++) {
            CategoryModel category = updates.get(i);
            CategoryEntity currentCategory = foundCate.get(i);
            if(!Objects.equals(category.getName(), currentCategory.getName())) {
                if(categoryRepository.existsByName(category.getName())) {
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "name");
                    conflict.put("value", category.getName());
                    conflict.put("message", "Name already exists");
                    conflicts.add(conflict);
                }
            }
        }   
        if(!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "CategoryModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<CategoryEntity> categoriesToUpdate = new ArrayList<>();
        Iterator<CategoryModel> categoryIterator = updates.iterator();
        Iterator<CategoryEntity> currentCategoryIterator = foundCate.iterator();

        while(categoryIterator.hasNext() && currentCategoryIterator.hasNext()) {
            CategoryModel update = categoryIterator.next();
            CategoryEntity current = currentCategoryIterator.next();

            Boolean hasChanges = !Objects.equals(update.getName(), current.getName()) ||
                                 !Objects.equals(update.getDescription(), current.getDescription()) ||
                                 !Objects.equals(update.getImage(), current.getImage()) ||
                                 !Objects.equals(update.getCategoryStatus(), current.getCategoryStatus());
            if(hasChanges) {
                modelMapper.map(update, current);
                categoriesToUpdate.add(current);
            }
        }

        if(!categoriesToUpdate.isEmpty()) {
            categoryRepository.saveAllAndFlush(categoriesToUpdate);
            log.logInfo("completed, updated " + categoriesToUpdate.size() + " categories", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, CATEGORY_REDIS_KEY_PREFIX);

        return foundCate.stream().map(
            entity -> modelMapper.map(entity, CategoryModel.class)
        ).collect(Collectors.toList());
    }

    // ======================================== Helper Methods ========================================
    
    private List<FilterCondition<CategoryEntity>> buildFilterConditions(
        Integer id, String name, CategoryStatus categoryStatus
    ) {
        List<FilterCondition<CategoryEntity>> conditions = new ArrayList<>();
        if(id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if(StringUtils.hasText(name)) {
            conditions.add(FilterCondition.likeIgnoreCase("name", name));
        }
        if(categoryStatus != null) {
            conditions.add(FilterCondition.eq("categoryStatus", categoryStatus));
        }
        return conditions;
    }

    // ======================================== Fallback Methods ========================================

    @SuppressWarnings("unused")
    private Page<CategoryModel> filtersFallback(
        Integer id, String name, CategoryStatus categoryStatus, Pageable pageable, Exception e
    ) {
        // là lỗi nghiệp vụ -> re-throw
        ResilienceFallbackUtils.rethrowBusinessThrowable(e);
        // nếu không phải lỗi circuit breaker open -> throw runtime exception
        if (!ResilienceFallbackUtils.isCircuitBreakerOpen(e)) {
            ResilienceFallbackUtils.throwAsRuntime(e);
        }

        // lấy thử data từ cache nếu có
        List<FilterCondition<CategoryEntity>> conditions = buildFilterConditions(
            id, name, categoryStatus
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            CATEGORY_REDIS_KEY_PREFIX, conditions, pageable);
            
        Page<CategoryModel> cachedPage = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, CategoryModel.class);

        if (cachedPage != null && !cachedPage.isEmpty()) {
            log.logInfo(
                "Found cache when calling fallback filters method, returning...", 
                getLogContext("filtersFallback", Collections.emptyList())
            );
            return cachedPage;
        }

        // lỗi circuit breaker open -> throw service unavailable exception
        throw ResilienceFallbackUtils.serviceUnavailable("filters", e);
    }

    @SuppressWarnings("unused")
    private List<CategoryModel> createsFallback(List<CategoryModel> categories, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "creates");
        return null;
    }

    @SuppressWarnings("unused")
    private List<CategoryModel> updatesFallback(List<CategoryModel> updates, List<Integer> categoryIds, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updates");
        return null;
    }

}
