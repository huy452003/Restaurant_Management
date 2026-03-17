package com.common.models.order;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.common.enums.OrderItemStatus;
import com.common.models.BaseModel;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemModel extends BaseModel{
    private Integer orderId;
    private Integer menuItemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private String specialInstructions;
    private OrderItemStatus orderItemStatus;
}
