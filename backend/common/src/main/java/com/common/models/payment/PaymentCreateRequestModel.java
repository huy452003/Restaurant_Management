package com.common.models.payment;

import java.math.BigDecimal;

import com.common.enums.PaymentMethod;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreateRequestModel {
    @NotNull(message = "validate.payment.orderId.required")
    @Min(value = 1, message = "validate.payment.orderId.min")
    private Integer orderId;
    @NotNull(message = "validate.payment.cashierId.required")
    @Min(value = 1, message = "validate.payment.cashierId.min")
    private Integer cashierId;
    @NotNull(message = "validate.payment.paymentMethod.required")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    @NotNull(message = "validate.payment.amount.required")
    @Min(value = 0, message = "validate.payment.amount.min")
    private BigDecimal amount;
}
