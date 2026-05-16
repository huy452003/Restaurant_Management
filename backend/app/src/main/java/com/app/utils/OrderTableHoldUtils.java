package com.app.utils;

import java.util.List;

import com.common.enums.OrderStatus;

/** Trạng thái đơn đang giữ bàn (khớp {@code TableStatusSyncServiceImp}). */
public final class OrderTableHoldUtils {

    public static final List<OrderStatus> TABLE_HOLDING_ORDER_STATUSES = List.of(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
        OrderStatus.PREPARING
    );

    private OrderTableHoldUtils() {
    }
}
