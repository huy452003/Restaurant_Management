package com.common.models.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả khi khởi tạo thanh toán VNPAY: URL redirect (sandbox/production) và snapshot payment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VnpayCheckoutResponse {
    /** URL đầy đủ (có query + vnp_SecureHash) để browser redirect tới cổng VNPAY. */
    private String paymentUrl;
    private VnpayCheckoutPaymentModel payment;
}
