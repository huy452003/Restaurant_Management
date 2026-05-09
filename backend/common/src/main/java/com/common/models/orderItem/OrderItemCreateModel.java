package com.common.models.orderItem;

import com.common.models.BaseModel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OrderItemCreateModel extends BaseModel {
    @NotBlank(message = "validate.order.orderNumber.required")
    @Size(min = 1, max = 50, message = "validate.order.orderNumber.size")
    private String orderNumber;

    @NotBlank(message = "validate.menuItem.name.required")
    @Size(min = 1, max = 100, message = "validate.menuItem.name.size")
    private String menuItemName;

    @NotNull(message = "validate.orderItem.quantity.required")
    @Min(value = 1, message = "validate.orderItem.quantity.min")
    private Integer quantity;

    @Size(max = 300, message = "validate.orderItem.specialInstructions.size")
    private String specialInstructions;
}
