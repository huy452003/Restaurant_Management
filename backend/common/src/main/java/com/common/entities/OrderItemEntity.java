package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

import com.common.enums.OrderStatus;

@Entity
@Table(name = "order_items")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEntity extends BaseEntity {
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
    private OrderStatus orderItemStatus;

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
