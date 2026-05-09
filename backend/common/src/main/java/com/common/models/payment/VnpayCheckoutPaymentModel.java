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

/**
 * Snapshot payment trả về khi khởi tạo VNPAY: dùng mã đơn và tên thu ngân thay cho id.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class VnpayCheckoutPaymentModel extends BaseModel {

    private String orderNumber;
    /** Tên thu ngân (cột fullname). */
    private String fullname;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String transactionId;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime paidAt;
}
