package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

import com.common.enums.OrderItemStatus;

@Entity
@Table(name = "order_items")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEntity extends BaseEntity {
    @Column(name = "order_id", nullable = false, insertable = false, updatable = false)
    private Integer orderId;
    @Column(name = "menu_item_id", nullable = false, insertable = false, updatable = false)
    private Integer menuItemId;
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;
    @Column(name = "sub_total", nullable = false)
    private BigDecimal subTotal;
    @Column(name = "special_instructions")
    private String specialInstructions;
    @Column(name = "order_item_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderItemStatus orderItemStatus;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity order;
    @ManyToOne
    @JoinColumn(name = "menu_item_id")
    private MenuItemEntity menuItem;
}
