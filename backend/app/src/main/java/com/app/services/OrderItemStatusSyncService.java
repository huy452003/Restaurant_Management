package com.app.services;

import com.common.enums.OrderStatus;

public interface OrderItemStatusSyncService {
    void syncItemsWithOrderStatus(Integer orderId, OrderStatus newStatus, OrderStatus previousStatus);
}
