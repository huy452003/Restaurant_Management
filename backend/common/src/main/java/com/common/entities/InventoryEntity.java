package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.common.enums.InventoryStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEntity extends BaseEntity {
    @Column(name = "ingredient_name", nullable = false, unique = true)
    private String ingredientName;
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    @Column(name = "unit", nullable = false)
    private String unit;
    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel;
    @Column(name = "inventory_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryStatus inventoryStatus;
    @Column(name = "last_restock_date", nullable = false)
    private LocalDateTime lastRestockDate;

    // version
    @Version
    @Column(name = "version")
    private Long version;
}
