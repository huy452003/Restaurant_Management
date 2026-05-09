package com.common.models.orderItem;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

import com.common.enums.OrderItemStatus;
import com.common.models.BaseModel;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderItemModel extends BaseModel{
    @NotBlank(message = "validate.order.orderNumber.required")
    @Size(min = 1, max = 50, message = "validate.order.orderNumber.size")
    private String orderNumber;
    @NotBlank(message = "validate.menuItem.name.required")
    @Size(min = 1, max = 100, message = "validate.menuItem.name.size")
    private String menuItemName;
    @NotNull(message = "validate.orderItem.quantity.required")
    @Min(value = 1, message = "validate.orderItem.quantity.min")
    private Integer quantity;
    @NotNull(message = "validate.orderItem.unitPrice.required")
    @Min(value = 0, message = "validate.orderItem.unitPrice.min")
    private BigDecimal unitPrice;
    @NotNull(message = "validate.orderItem.subTotal.required")
    @Min(value = 0, message = "validate.orderItem.subTotal.min")
    private BigDecimal subTotal;
    @Size(max = 300, message = "validate.orderItem.specialInstructions.size")
    private String specialInstructions;
    @NotNull(message = "validate.orderItem.orderItemStatus.required")
    @Enumerated(EnumType.STRING)
    private OrderItemStatus orderItemStatus;
}
