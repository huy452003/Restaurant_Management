package com.app.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.CategoryStatus;
import com.common.models.category.CategoryModel;

public interface CategoryService {
    Page<CategoryModel> filters(
        Integer id, String name, CategoryStatus categoryStatus, Pageable pageable
    );
    List<CategoryModel> create(List<CategoryModel> categories);
    List<CategoryModel> update(List<CategoryModel> updates, List<Integer> categoryIds);
}
