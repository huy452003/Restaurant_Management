package com.common.models.category;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.common.enums.MenuItemStatus;
import com.common.models.BaseModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryModel extends BaseModel{
    private String name;
    private String description;
    private String image;
    private MenuItemStatus menuItemStatus;
}
