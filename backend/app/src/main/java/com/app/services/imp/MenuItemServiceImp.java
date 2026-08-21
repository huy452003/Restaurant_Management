package com.app.services.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.Collections;
import org.springframework.util.StringUtils;

import org.modelmapper.ModelMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.services.MenuItemService;
import com.common.entities.CategoryEntity;
import com.common.entities.MenuItemEntity;
import com.common.enums.MenuItemStatus;
import com.common.models.menu.MenuItemModel;
import com.common.repositories.CategoryRepository;
import com.common.repositories.MenuItemRepository;
import com.common.specifications.FilterCondition;
import com.common.specifications.SpecificationHelper;
import com.common.utils.FilterPageCacheFacade;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.handle_exceptions.support.ResilienceFallbackUtils;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImp implements MenuItemService{
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LoggingService log;
    private final ModelMapper modelMapper;

    private LogContext getLogContext(String methodName, List<Integer> menuItemIds){
        return LogContext.builder()
                .module("app")
                .className(this.getClass().getSimpleName())
                .methodName(methodName)
                .ids(menuItemIds)
                .build();
    }

    private static final String MENU_ITEM_REDIS_KEY_PREFIX = "menu-item:";

    @Override
    @CircuitBreaker(name = "menu-item-service-read", fallbackMethod = "filtersFallback")
    public Page<MenuItemModel> filters(
        Integer id, String name, String categoryName,
        MenuItemStatus menuItemStatus, Pageable pageable
    ) {
        LogContext logContext = getLogContext("filters", Collections.emptyList());
        log.logInfo("Filtering menu items with pagination ...!", logContext);

        List<FilterCondition<MenuItemEntity>> conditions = buildFilterConditions(
            id, name, categoryName, menuItemStatus
        );
        
        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            MENU_ITEM_REDIS_KEY_PREFIX, conditions, pageable);
        
        Page<MenuItemModel> cached = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, MenuItemModel.class);
        
        if (cached != null && !cached.isEmpty()) {
            log.logInfo("found " + cached.getTotalElements() + " menu items in cache", logContext);
            return cached;
        }

        Page<MenuItemEntity> pageEntities;
        if(conditions.isEmpty()){
            pageEntities = menuItemRepository.findAll(pageable);
            log.logWarn("No conditions provided, returning all menu items with pagination", logContext);
        } else {
            Specification<MenuItemEntity> spec = SpecificationHelper.buildSpecification(conditions);
            pageEntities = menuItemRepository.findAll(spec, pageable);
        }

        List<MenuItemModel> pageDatas = pageEntities.getContent().stream().map(
            this::toMenuItemModel
        ).collect(Collectors.toList());

        Page<MenuItemModel> MenuItemModelPage = new PageImpl<>(
            pageDatas, pageEntities.getPageable(), pageEntities.getTotalElements()
        );

        if (redisKeyFilters != null) {
            FilterPageCacheFacade.writeFirstPageCache(redisTemplate, redisKeyFilters, MenuItemModelPage);
            log.logInfo("cached first-page filter snapshot for " + MenuItemModelPage.getTotalElements()
                + " menu items, key: " + redisKeyFilters, logContext);
        }
        return MenuItemModelPage;
    }

    @Override
    @CircuitBreaker(name = "menu-item-service-write", fallbackMethod = "createsFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public List<MenuItemModel> create(List<MenuItemModel> menuItems) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating menu items ...!", logContext);

        Map<String, CategoryEntity> categoriesByName = menuItems.stream()
            .map(MenuItemModel::getCategoryName)
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(), 
                categoryName -> resolveCategory(categoryName, logContext)
            )
        );

        List<Object> conflicts = new ArrayList<>();
        for(MenuItemModel menuItem : menuItems) {
            if(menuItemRepository.existsByName(menuItem.getName())) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("field", "name");
                conflict.put("value", menuItem.getName());
                conflict.put("message", "Name already exists");
                conflicts.add(conflict);
            }
        }
        if(!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "MenuItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<MenuItemEntity> menuItemEntities = menuItems.stream().map(
            menuItemModel -> {
                MenuItemEntity entity = modelMapper.map(menuItemModel, MenuItemEntity.class);
                entity.setCategory(categoriesByName.get(menuItemModel.getCategoryName()));
                return entity;
            }
        ).collect(Collectors.toList());

        menuItemRepository.saveAll(menuItemEntities);

        // xóa cache filter
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, MENU_ITEM_REDIS_KEY_PREFIX);
        log.logInfo("Deleted filter caches after create", logContext);
        
        log.logInfo("completed, created " + menuItemEntities.size() + " menu items", logContext);
        return menuItemEntities.stream().map(
            this::toMenuItemModel
        ).collect(Collectors.toList());
    }

    @Override
    @CircuitBreaker(name = "menu-item-service-write", fallbackMethod = "updatesFallback")
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    @Retryable(value = {OptimisticLockingFailureException.class}, maxAttempts = 3)
    public List<MenuItemModel> update(List<MenuItemModel> updates, List<Integer> menuItemIds) {
        LogContext logContext = getLogContext("update", menuItemIds);
        log.logInfo("Updating menu items ...!", logContext);

        if(updates.size() != menuItemIds.size()){
            ValidationExceptionHandle e = new ValidationExceptionHandle(
                "Size mismatch between updates and menuItemIds",
                menuItemIds,
                "MenuItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<MenuItemEntity> foundMenuItems = menuItemIds.stream().map(
            id -> menuItemRepository.findById(id).orElseThrow(() -> {
                NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                    "MenuItem not found with id: " + id, 
                    Collections.singletonList(id), 
                    "MenuItemModel"
                );
                log.logError(e.getMessage(), e, logContext);
                return e;
            }
            )
        ).collect(Collectors.toList());
        log.logInfo("found " + foundMenuItems.size() + " menu items", logContext);

        List<Object> conflicts = new ArrayList<>();
        for(int i = 0; i < updates.size(); i++) {
            MenuItemModel menuItem = updates.get(i);
            MenuItemEntity currentMenuItem = foundMenuItems.get(i);
            if(!Objects.equals(menuItem.getName(), currentMenuItem.getName())) {
                if(menuItemRepository.existsByName(menuItem.getName())) {
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", "name");
                    conflict.put("value", menuItem.getName());
                    conflict.put("message", "Name already exists");
                    conflicts.add(conflict);
                }
            }
        }
        if(!conflicts.isEmpty()) {
            ConflictExceptionHandle e = new ConflictExceptionHandle(
                "Duplicate unique fields detected",
                conflicts,
                "MenuItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }
        
        List<MenuItemEntity> menuItemsToUpdate = new ArrayList<>();
        Iterator<MenuItemModel> menuItemIterator = updates.iterator();
        Iterator<MenuItemEntity> currentMenuItemIterator = foundMenuItems.iterator();
        while (menuItemIterator.hasNext() && currentMenuItemIterator.hasNext()) {
            MenuItemModel update = menuItemIterator.next();
            MenuItemEntity current = currentMenuItemIterator.next();
            
            Boolean hasChanges = !Objects.equals(update.getName(), current.getName()) ||
                                 !Objects.equals(update.getDescription(), current.getDescription()) ||
                                 !Objects.equals(update.getPrice(), current.getPrice()) ||
                                 !Objects.equals(update.getImage(), current.getImage()) ||
                                 !Objects.equals(update.getCategoryName(), current.getCategory().getName()) ||
                                 !Objects.equals(update.getMenuItemStatus(), current.getMenuItemStatus());
            if(hasChanges) {
                modelMapper.map(update, current);
                current.setCategory(resolveCategory(update.getCategoryName(), logContext));
                menuItemsToUpdate.add(current);
            }
        }

        if(!menuItemsToUpdate.isEmpty()) {
            menuItemRepository.saveAllAndFlush(menuItemsToUpdate);
            log.logInfo("completed, updated " + menuItemsToUpdate.size() + " menu items", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        // xóa cache filter
        FilterPageCacheFacade.clearFirstPageCache(redisTemplate, MENU_ITEM_REDIS_KEY_PREFIX);
        log.logInfo("Deleted filter caches after update", logContext);

        return foundMenuItems.stream().map(
            this::toMenuItemModel
        ).collect(Collectors.toList());
    }

    // ======================================== Helper Methods ========================================

    private MenuItemModel toMenuItemModel(MenuItemEntity entity) {
        MenuItemModel menuItemModel = modelMapper.map(entity, MenuItemModel.class);
        if(entity.getCategory() != null) {
            menuItemModel.setCategoryName(entity.getCategory().getName());
        }
        return menuItemModel;
    }

    private CategoryEntity resolveCategory(String categoryName, LogContext logContext) {
        return categoryRepository.findByName(categoryName).orElseThrow(() -> {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Category not found with name: " + categoryName, 
                Collections.singletonList(categoryName), 
                "CategoryModel"
            );
            log.logError(e.getMessage(), e, logContext);
            return e;
        });
    }

    private List<FilterCondition<MenuItemEntity>> buildFilterConditions(
        Integer id, String name, String categoryName, MenuItemStatus menuItemStatus
    ) {
        List<FilterCondition<MenuItemEntity>> conditions = new ArrayList<>();
        if (id != null && id > 0) {
            conditions.add(FilterCondition.eq("id", id));
        }
        if (StringUtils.hasText(name)) {
            conditions.add(FilterCondition.likeIgnoreCase("name", name));
        }
        if (StringUtils.hasText(categoryName)) {
            conditions.add(FilterCondition.likeIgnoreCase("category.name", categoryName));
        }
        if (menuItemStatus != null) {
            conditions.add(FilterCondition.eq("menuItemStatus", menuItemStatus));
        }
        return conditions;
    }

    // ======================================== Fallback Methods ========================================

    @SuppressWarnings("unused")
    private Page<MenuItemModel> filtersFallback(
        Integer id, String name, String categoryName, MenuItemStatus menuItemStatus, Pageable pageable, Exception e
    ) {
        // là lỗi nghiệp vụ -> re-throw
        ResilienceFallbackUtils.rethrowBusinessThrowable(e);
        // nếu không phải lỗi circuit breaker open -> throw runtime exception
        if (!ResilienceFallbackUtils.isCircuitBreakerOpen(e)) {
            ResilienceFallbackUtils.throwAsRuntime(e);
        }

        // lấy thử data từ cache nếu có
        List<FilterCondition<MenuItemEntity>> conditions = buildFilterConditions(
            id, name, categoryName, menuItemStatus
        );

        String redisKeyFilters = FilterPageCacheFacade.buildFirstPageKeyIfApplicable(
            MENU_ITEM_REDIS_KEY_PREFIX, conditions, pageable);
            
        Page<MenuItemModel> cachedPage = FilterPageCacheFacade.readFirstPageCache(
            redisTemplate, redisKeyFilters, pageable, objectMapper, MenuItemModel.class);

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
    private List<MenuItemModel> createsFallback(List<MenuItemModel> menuItems, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "creates");
        return null;
    }

    @SuppressWarnings("unused")
    private List<MenuItemModel> updatesFallback(List<MenuItemModel> updates, List<Integer> menuItemIds, Exception e) {
        ResilienceFallbackUtils.propagateCircuitBreakerFailure(e, "updates");
        return null;
    }
}
