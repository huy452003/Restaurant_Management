package com.app.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.MenuItemStatus;
import com.common.models.menu.MenuItemModel;

public interface MenuItemService {
    Page<MenuItemModel> filters(
        Integer id, String name, String categoryName,
        MenuItemStatus menuItemStatus, Pageable pageable
    );
    List<MenuItemModel> create(List<MenuItemModel> menuItems);
    List<MenuItemModel> update(List<MenuItemModel> updates, List<Integer> menuItemIds);
}
