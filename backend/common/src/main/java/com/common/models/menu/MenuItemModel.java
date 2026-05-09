package com.common.models.menu;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

import com.common.enums.MenuItemStatus;
import com.common.models.BaseModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemModel extends BaseModel{
    @NotBlank(message = "validate.menuItem.name.required")
    @Size(min = 3, max = 100, message = "validate.menuItem.name.size")
    private String name;
    @Size(max = 255, message = "validate.menuItem.description.size")
    private String description;
    @NotNull(message = "validate.menuItem.price.required")
    @DecimalMin(value = "0", message = "validate.menuItem.price.min")
    private BigDecimal price;
    @NotBlank(message = "validate.menuItem.image.required")
    private String image;
    @NotBlank(message = "validate.menuItem.categoryName.required")
    private String categoryName;
    @NotNull(message = "validate.menuItem.menuItemStatus.required")
    @Enumerated(EnumType.STRING)    
    private MenuItemStatus menuItemStatus;
}
