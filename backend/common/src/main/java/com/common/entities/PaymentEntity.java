package com.common.entities;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;

@Entity
@Table(name = "payments")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity extends BaseEntity {
    @Column(name = "order_id", nullable = false, insertable = false, updatable = false)
    private Integer orderId;
    @Column(name = "cashier_id", nullable = false, insertable = false, updatable = false)
    private Integer cashierId;
    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    @Column(name = "transaction_id")
    private String transactionId;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // version
    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity order;
    @ManyToOne
    @JoinColumn(name = "cashier_id")
    private UserEntity cashier;
}
