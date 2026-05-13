package com.common.models.payment;

import com.common.enums.PaymentMethod;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreateRequestModel {
    @NotBlank(message = "validate.payment.orderNumber.required")
    @Size(min = 1, max = 50, message = "validate.payment.orderNumber.size")
    private String orderNumber;
    @NotNull(message = "validate.payment.paymentMethod.required")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
}
