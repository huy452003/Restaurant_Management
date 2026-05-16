package com.app.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.common.entities.OrderItemEntity;
import com.common.enums.OrderStatus;

public final class OrderItemStatusSyncUtils {

    private OrderItemStatusSyncUtils() {
    }

    /**
     * Đồng bộ trạng thái món theo đơn khi đơn đổi trạng thái (nguồn: cập nhật đơn / hủy / hoàn thành).
     * Món đã {@link OrderStatus#CANCELLED} giữ nguyên, trừ khi cả đơn bị hủy.
     */
    public static List<OrderItemEntity> syncItemsWithOrderStatus(
        List<OrderItemEntity> items,
        OrderStatus orderStatus
    ) {
        if (items == null || items.isEmpty() || orderStatus == null) {
            return List.of();
        }
        List<OrderItemEntity> changed = new ArrayList<>();
        for (OrderItemEntity item : items) {
            OrderStatus current = item.getOrderItemStatus();
            OrderStatus target = resolveItemStatusForOrder(current, orderStatus);
            if (!Objects.equals(current, target)) {
                item.setOrderItemStatus(target);
                changed.add(item);
            }
        }
        return changed;
    }

    static OrderStatus resolveItemStatusForOrder(OrderStatus current, OrderStatus orderStatus) {
        if (orderStatus == OrderStatus.CANCELLED) {
            return OrderStatus.CANCELLED;
        }
        if (current == OrderStatus.CANCELLED) {
            return OrderStatus.CANCELLED;
        }
        if (orderStatus == OrderStatus.COMPLETED) {
            return OrderStatus.COMPLETED;
        }
        return orderStatus;
    }
}
