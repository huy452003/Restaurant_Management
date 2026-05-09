package com.common.models.order;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.common.enums.OrderStatus;
import com.common.enums.OrderType;
import com.common.models.BaseModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderModel extends BaseModel{
    private String orderNumber;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Integer tableNumber;
    private Integer waiterId;
    private OrderStatus orderStatus;
    private OrderType orderType;
    private BigDecimal subTotal;
    private BigDecimal tax;
    private BigDecimal totalAmount;
    private Integer totalOrderItem;
    private String notes;
    private LocalDateTime completedAt;
}
