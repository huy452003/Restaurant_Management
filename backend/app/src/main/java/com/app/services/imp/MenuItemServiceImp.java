package com.app.services.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Collections;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.app.services.MenuItemService;
import com.common.entities.MenuItemEntity;
import com.common.enums.MenuItemStatus;
import com.common.models.menu.MenuItemModel;
import com.common.repositories.MenuItemRepository;
import com.handle_exceptions.NotFoundExceptionHandle;
import com.handle_exceptions.ConflictExceptionHandle;
import com.handle_exceptions.ValidationExceptionHandle;
import com.logging.models.LogContext;
import com.logging.services.LoggingService;

@Service
public class MenuItemServiceImp implements MenuItemService{
    @Autowired
    private MenuItemRepository menuItemRepo;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private LoggingService log;
    @Autowired
    private ModelMapper modelMapper;

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
    public List<MenuItemModel> getAll() {
        LogContext logContext = getLogContext("getAll", Collections.emptyList());
        log.logInfo("Getting all menu items ...!", logContext);

        String redisKeyGetAllMenuItems = MENU_ITEM_REDIS_KEY_PREFIX + "getAll";
        List<MenuItemModel> cached = (List<MenuItemModel>) redisTemplate.opsForValue().get(redisKeyGetAllMenuItems);
        if(cached != null && !cached.isEmpty()){
            log.logInfo("found " + cached.size() + " menu items in cache", logContext);
            return cached;
        }
        log.logInfo("Not found menu items in cache, query from database", logContext);

        List<MenuItemEntity> menuItemEntities = menuItemRepo.findAll();
        if(menuItemEntities == null || menuItemEntities.isEmpty()) {
            NotFoundExceptionHandle e = new NotFoundExceptionHandle(
                "Not found MenuItems in database", 
                menuItemEntities, 
                "MenuItemModel"
            );
            log.logError(e.getMessage(), e, logContext);
            throw e;
        }

        List<MenuItemModel> menuItemModels = menuItemEntities.stream().map(
            entity -> modelMapper.map(entity, MenuItemModel.class)
        ).collect(Collectors.toList());

        redisTemplate.opsForValue().set(redisKeyGetAllMenuItems, menuItemModels);
        log.logInfo("completed, found " + menuItemModels.size() + " menu items", logContext);
        log.logInfo("cached " + menuItemModels.size() + " menu items in key: " + redisKeyGetAllMenuItems, logContext);
        return menuItemModels;
    }

    @Override
    public List<MenuItemModel> create(List<MenuItemModel> menuItems) {
        LogContext logContext = getLogContext("create", Collections.emptyList());
        log.logInfo("Creating menu items ...!", logContext);

        List<Object> conflicts = new ArrayList<>();
        for(MenuItemModel menuItem : menuItems) {
            if(menuItemRepo.existsByName(menuItem.getName())) {
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
            model -> {
                if(model.getMenuItemStatus() == null) {
                    model.setMenuItemStatus(MenuItemStatus.AVAILABLE);
                }
                return modelMapper.map(model, MenuItemEntity.class);
            }
        ).collect(Collectors.toList());

        menuItemRepo.saveAll(menuItemEntities);

        // xóa cache
        redisTemplate.delete(MENU_ITEM_REDIS_KEY_PREFIX + "getAll");
        log.logInfo("Deleted cache for getAll", logContext);
        log.logInfo("completed, created " + menuItemEntities.size() + " menu items", logContext);
        return menuItemEntities.stream().map(
            entity -> modelMapper.map(entity, MenuItemModel.class)
        ).collect(Collectors.toList());
    }

    @Override
    public List<MenuItemModel> update(List<MenuItemModel> updates, List<Integer> menuItemIds) {
        LogContext logContext = getLogContext(
            "update", 
            menuItemIds != null ? menuItemIds : Collections.emptyList()
        );
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
            id -> menuItemRepo.findById(id).orElseThrow(() -> {
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
            if(!menuItem.getName().equals(currentMenuItem.getName())) {
                if(menuItemRepo.existsByName(menuItem.getName())) {
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
            
            Boolean hasChanges = !update.getName().equals(current.getName()) ||
                                 !update.getDescription().equals(current.getDescription()) ||
                                 !update.getPrice().equals(current.getPrice()) ||
                                 !update.getImage().equals(current.getImage()) ||
                                 !update.getCategoryName().equals(current.getCategoryName()) ||
                                 !update.getMenuItemStatus().equals(current.getMenuItemStatus());
            if(hasChanges) {
                modelMapper.map(update, current);
                menuItemsToUpdate.add(current);
            }
        }

        if(!menuItemsToUpdate.isEmpty()) {
            menuItemRepo.saveAll(menuItemsToUpdate);
            log.logInfo("completed, updated " + menuItemsToUpdate.size() + " menu items", logContext);
        } else {
            log.logInfo("completed, no changes detected, skipped update", logContext);
        }

        // xóa cache
        redisTemplate.delete(MENU_ITEM_REDIS_KEY_PREFIX + "getAll");
        log.logInfo("Deleted cache for getAll", logContext);

        return foundMenuItems.stream().map(
            entity -> modelMapper.map(entity, MenuItemModel.class)
        ).collect(Collectors.toList());
    }
}
