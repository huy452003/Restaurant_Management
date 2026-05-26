package com.app.utils;

import java.util.List;

import com.common.enums.OrderStatus;

public final class OrderTableHoldUtils {

    private OrderTableHoldUtils() {
    }

    // các trạng thái đơn đang giữ bàn
    public static final List<OrderStatus> TABLE_HOLDING_ORDER_STATUSES = List.of(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
        OrderStatus.PREPARING
    );

}
