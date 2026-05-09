package com.common.models.orderItem;

import com.common.models.BaseModel;
import jakarta.validation.constraints.Min;
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
public class OrderItemCustomerUpdateModel extends BaseModel {
    @NotNull(message = "validate.orderItem.quantity.required")
    @Min(value = 1, message = "validate.orderItem.quantity.min")
    private Integer quantity;

    @Size(max = 300, message = "validate.orderItem.specialInstructions.size")
    private String specialInstructions;
}
