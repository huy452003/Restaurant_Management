package com.app.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.common.enums.OrderStatus;
import com.common.enums.OrderType;
import com.common.models.order.OrderAdminRequestModel;
import com.common.models.order.OrderCustomerRequestModel;
import com.common.models.order.OrderModel;

public interface OrderService {
    Page<OrderModel> filtersForCustomer(
        Integer id, String orderNumber, Integer tableNumber,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount,
        Pageable pageable
    );
    Page<OrderModel> filtersForAdmin(
        Integer id, String orderNumber, Integer tableNumber, Integer waiterId,
        String customerName, String customerPhone, String customerEmail,
        OrderStatus orderStatus, OrderType orderType, BigDecimal subTotal,
        BigDecimal tax, BigDecimal totalAmount,
        Pageable pageable
    );
    OrderModel create(OrderCustomerRequestModel order);
    OrderModel updateForCustomer(OrderCustomerRequestModel update, Integer orderId);
    List<OrderModel> updateByAdmin(List<OrderAdminRequestModel> updates, List<Integer> orderIds);
    OrderModel submit(Integer orderId);
    OrderModel cancel(Integer orderId);

    // Hủy đơn PENDING quá hạn (scheduler) — trả bàn cho khách/đặt bàn khác.
    int expireStalePendingOrders();

    /** CONFIRMED chưa thanh toán đủ (DINE_IN + DELIVERY): hủy đơn + payment treo sau TTL. */
    int expireStaleConfirmedUnpaidOrders();
}
