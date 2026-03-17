package com.app.services;

import java.util.List;

import com.common.models.menu.MenuItemModel;

public interface MenuItemService {
    List<MenuItemModel> getAll();
    List<MenuItemModel> create(List<MenuItemModel> menuItems);
    List<MenuItemModel> update(List<MenuItemModel> updates, List<Integer> menuItemIds);
}
