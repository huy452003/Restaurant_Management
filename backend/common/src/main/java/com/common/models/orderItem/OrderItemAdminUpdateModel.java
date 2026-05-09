package com.common.models.orderItem;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

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
public class OrderItemAdminUpdateModel extends BaseModel{
    @NotBlank(message = "validate.menuItem.name.required")
    @Size(min = 1, max = 100, message = "validate.menuItem.name.size")
    private String menuItemName;
    @NotNull(message = "validate.orderItem.quantity.required")
    @Min(value = 1, message = "validate.orderItem.quantity.min")
    private Integer quantity;
    @Size(max = 300, message = "validate.orderItem.specialInstructions.size")
    private String specialInstructions;
    @NotNull(message = "validate.orderItem.orderItemStatus.required")
    @Enumerated(EnumType.STRING)
    private OrderItemStatus orderItemStatus;
}
