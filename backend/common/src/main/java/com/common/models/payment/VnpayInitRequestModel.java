package com.common.models.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VnpayInitRequestModel {
    @NotBlank(message = "validate.order.orderNumber.required")
    @Size(min = 1, max = 50, message = "validate.order.orderNumber.size")
    private String orderNumber;
}
