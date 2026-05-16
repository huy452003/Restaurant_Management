package com.app.services;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.entities.OrderEntity;
import com.common.enums.PaymentMethod;
import com.common.enums.PaymentStatus;
import com.common.models.payment.PaymentCreateRequestModel;
import com.common.models.payment.PaymentModel;

public interface PaymentService {
    Page<PaymentModel> filters(
        Integer id, String orderNumber, String cashierFullname,
        PaymentMethod paymentMethod, BigDecimal amount,
        PaymentStatus paymentStatus, String transactionId,
        Pageable pageable
    );
    PaymentModel create(PaymentCreateRequestModel payment);
    PaymentModel complete(Integer paymentId);
    PaymentModel cancel(Integer paymentId);

    void cancelPendingPaymentsForOrder(Integer orderId);

    boolean canAcceptNewPayment(OrderEntity order, int orderItemCount, BigDecimal allocatedAmount);

    // Đánh dấu PENDING thành FAILED khi cổng VNPAY báo không thành công (IPN/return)
    // không dùng cho hủy thủ công.
    PaymentModel markFailedFromGateway(Integer paymentId);
}
