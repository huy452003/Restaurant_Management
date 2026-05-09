package com.common.models.category;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.common.enums.CategoryStatus;
import com.common.models.BaseModel;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryModel extends BaseModel{
    @NotBlank(message = "validate.category.name.required")
    @Size(min = 3, max = 50, message = "validate.category.name.size")
    private String name;
    @Size(max = 255, message = "validate.category.description.size")
    private String description;
    @NotBlank(message = "validate.category.image.required")
    private String image;
    @NotNull(message = "validate.category.categoryStatus.required")
    @Enumerated(EnumType.STRING)
    private CategoryStatus categoryStatus;
}
