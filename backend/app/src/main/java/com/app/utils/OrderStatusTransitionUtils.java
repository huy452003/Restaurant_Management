package com.app.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.common.entities.OrderEntity;
import com.common.enums.OrderStatus;
import com.handle_exceptions.ValidationExceptionHandle;

/**
 * Pipeline đơn: PENDING → CONFIRMED → PREPARING → COMPLETED.
 * <p>
 * CONFIRMED → PREPARING: chỉ hệ thống sau khi thanh toán đủ ({@link #applyPreparingAfterFullPayment}).
 * PREPARING → COMPLETED: staff (đã có payment COMPLETED).
 * CANCELLED: chỉ khi chưa có payment COMPLETED.
 */
public final class OrderStatusTransitionUtils {

    private static final List<OrderStatus> FORWARD_PIPELINE = Arrays.asList(
        OrderStatus.PENDING,
        OrderStatus.CONFIRMED,
        OrderStatus.PREPARING
    );

    private OrderStatusTransitionUtils() {
    }

    public static void applyOrderStatusTransition(OrderEntity order, OrderStatus targetStatus) {
        applyOrderStatusTransition(order, targetStatus, false);
    }

    public static void applyOrderStatusTransition(
        OrderEntity order,
        OrderStatus targetStatus,
        boolean hasCompletedPayment
    ) {
        OrderStatus oldStatus = order.getOrderStatus();
        if (!isAllowedTransition(oldStatus, targetStatus, hasCompletedPayment)) {
            throw new ValidationExceptionHandle(
                "Invalid order status transition from " + oldStatus + " to " + targetStatus,
                Collections.singletonList(targetStatus),
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

    /** Sau thanh toán đủ tiền khi đơn đang CONFIRMED — không qua form staff. */
    public static boolean applyPreparingAfterFullPayment(OrderEntity order) {
        if (order == null || order.getOrderStatus() != OrderStatus.CONFIRMED) {
            return false;
        }
        order.setOrderStatus(OrderStatus.PREPARING);
        return true;
    }

    public static boolean isAllowedTransition(
        OrderStatus oldStatus,
        OrderStatus targetStatus,
        boolean hasCompletedPayment
    ) {
        if (oldStatus == null || targetStatus == null) {
            return false;
        }
        if (Objects.equals(oldStatus, targetStatus)) {
            return true;
        }
        if (oldStatus == OrderStatus.CANCELLED || oldStatus == OrderStatus.COMPLETED) {
            return false;
        }
        if (targetStatus == OrderStatus.CANCELLED) {
            return !hasCompletedPayment;
        }
        if (targetStatus == OrderStatus.COMPLETED) {
            return oldStatus == OrderStatus.PREPARING && hasCompletedPayment;
        }
        if (oldStatus == OrderStatus.CONFIRMED && targetStatus == OrderStatus.PREPARING) {
            return false;
        }
        int fromIdx = FORWARD_PIPELINE.indexOf(oldStatus);
        int toIdx = FORWARD_PIPELINE.indexOf(targetStatus);
        if (fromIdx < 0 || toIdx < 0) {
            return false;
        }
        return toIdx > fromIdx;
    }

    public static List<OrderStatus> allowedTargetStatuses(
        OrderStatus current,
        boolean hasCompletedPayment
    ) {
        if (current == null) {
            return List.of();
        }
        List<OrderStatus> allowed = new ArrayList<>();
        for (OrderStatus candidate : OrderStatus.values()) {
            if (Objects.equals(current, candidate)) {
                continue;
            }
            if (isAllowedTransition(current, candidate, hasCompletedPayment)) {
                allowed.add(candidate);
            }
        }
        if (current != null && !allowed.contains(current)) {
            allowed.add(0, current);
        }
        return allowed;
    }

    public static boolean canCustomerCancel(OrderStatus current, boolean hasCompletedPayment) {
        if (current != OrderStatus.PENDING && current != OrderStatus.CONFIRMED) {
            return false;
        }
        return isAllowedTransition(current, OrderStatus.CANCELLED, hasCompletedPayment);
    }
}
