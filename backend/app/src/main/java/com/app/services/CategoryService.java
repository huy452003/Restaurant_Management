package com.app.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.MenuItemStatus;
import com.common.models.category.CategoryModel;

public interface CategoryService {
    Page<CategoryModel> filters(
        Integer id,
        String name,
        MenuItemStatus menuItemStatus,
        Pageable pageable
    );
    List<CategoryModel> create(List<CategoryModel> categories);
    List<CategoryModel> update(List<CategoryModel> updates, List<Integer> categoryIds);
}
