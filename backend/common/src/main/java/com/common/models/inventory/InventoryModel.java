package com.common.models.inventory;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.common.enums.InventoryStatus;
import com.common.models.BaseModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryModel extends BaseModel{
    private String ingredientName;
    private Integer quantity;
    private String unit;
    private Integer minStockLevel;
    private InventoryStatus inventoryStatus;
    private LocalDateTime lastRestockDate;
}
