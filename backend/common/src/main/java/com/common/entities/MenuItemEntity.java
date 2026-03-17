package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import com.common.enums.MenuItemStatus;

@Entity
@Table(name = "menu_items")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemEntity extends BaseEntity {
    @Column(name = "name", unique = true)
    private String name;
    @Column(name = "description")
    private String description;
    @Column(name = "price")
    private BigDecimal price;
    @Column(name = "image")
    private String image;
    @Column(name = "category_name")
    private String categoryName;
    @Column(name = "menu_item_status")
    private MenuItemStatus menuItemStatus;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne
    @JoinColumn(name = "category_name", insertable = false, updatable = false)
    private CategoryEntity category;
    @OneToMany(mappedBy = "menuItem", fetch = FetchType.LAZY)
    private List<OrderItemEntity> orderItems;
}
