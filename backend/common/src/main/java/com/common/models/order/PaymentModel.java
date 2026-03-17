package com.common.models.order;

import java.math.BigDecimal;

import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.models.BaseModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentModel extends BaseModel{
    private Integer orderId;
    private Integer cashierId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
}
