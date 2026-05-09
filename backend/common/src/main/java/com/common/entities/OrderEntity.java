package com.common.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.common.enums.OrderStatus;
import com.common.enums.OrderType;

@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity extends BaseEntity {
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;
    @Column(name = "customer_name")
    private String customerName;
    @Column(name = "customer_phone")
    private String customerPhone;
    @Column(name = "customer_email")
    private String customerEmail;
    @Column(name = "table_number", nullable = false, insertable = false, updatable = false)
    private Integer tableNumber;
    @Column(name = "waiter_id", nullable = true, insertable = false, updatable = false)
    private Integer waiterId;
    @Column(name = "order_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @Column(name = "order_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    @Column(name = "sub_total")
    private BigDecimal subTotal;
    @Column(name = "tax")
    private BigDecimal tax;
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    @Column(name = "notes")
    private String notes;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne
    @JoinColumn(name = "table_number", referencedColumnName = "table_number")
    private TableEntity table;
    @ManyToOne
    @JoinColumn(name = "waiter_id")
    private UserEntity waiter;
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<OrderItemEntity> orderItems;
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<PaymentEntity> payments;
}
