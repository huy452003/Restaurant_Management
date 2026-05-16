package com.common.models.order;

import com.common.enums.OrderType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCustomerRequestModel {
    @Min(value = 1, message = "validate.order.tableNumber.min")
    private Integer tableNumber;
    @NotNull(message = "validate.order.orderType.required")
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    @Size(max = 300, message = "validate.order.notes.size")
    private String notes;
}
