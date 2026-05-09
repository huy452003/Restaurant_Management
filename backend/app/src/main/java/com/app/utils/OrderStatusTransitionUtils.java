package com.app.utils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.common.entities.OrderEntity;
import com.common.enums.OrderStatus;
import com.handle_exceptions.ValidationExceptionHandle;

public final class OrderStatusTransitionUtils {

    private OrderStatusTransitionUtils() {
    }

    public static void applyOrderStatusTransition(OrderEntity order, OrderStatus targetStatus) {
        OrderStatus oldStatus = order.getOrderStatus();
        if (!isAllowedTransition(oldStatus, targetStatus)) {
            throw new ValidationExceptionHandle(
                "Invalid order status transition from " + oldStatus + " to " + targetStatus,
                java.util.Collections.singletonList(targetStatus),
                "OrderModel"
            );
        }
        order.setOrderStatus(targetStatus);

        if (!Objects.equals(oldStatus, targetStatus)) {
            if (targetStatus == OrderStatus.COMPLETED || targetStatus == OrderStatus.CANCELLED) {
                order.setCompletedAt(LocalDateTime.now());
            } else if (oldStatus == OrderStatus.COMPLETED || oldStatus == OrderStatus.CANCELLED) {
                order.setCompletedAt(null);
            }
        }
    }

    private static boolean isAllowedTransition(OrderStatus oldStatus, OrderStatus targetStatus) {
        if (oldStatus == null || targetStatus == null) {
            return false;
        }
        if (Objects.equals(oldStatus, targetStatus)) {
            return true;
        }
        if (oldStatus == OrderStatus.CANCELLED || oldStatus == OrderStatus.COMPLETED) {
            return false;
        }
        Set<OrderStatus> nonTerminalStatuses = EnumSet.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.SERVED
        );
        return nonTerminalStatuses.contains(targetStatus) ||
            targetStatus == OrderStatus.CANCELLED ||
            targetStatus == OrderStatus.COMPLETED;
    }
}
