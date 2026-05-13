package com.common.models.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.models.BaseModel;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class PaymentModel extends BaseModel{
    private String orderNumber;
    private String cashierFullname;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String transactionId;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime paidAt;
}
