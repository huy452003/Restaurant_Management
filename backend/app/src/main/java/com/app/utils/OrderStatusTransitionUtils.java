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

    // áp dụng trạng thái mới với đơn chưa thanh toán đủ
    public static void applyOrderStatusTransition(OrderEntity order, OrderStatus targetStatus) {
        applyOrderStatusTransition(order, targetStatus, false);
    }

    // áp dụng trạng thái mới của order
    public static void applyOrderStatusTransition(
        OrderEntity order,
        OrderStatus targetStatus,
        boolean hasCompletedPayment
    ) {
        OrderStatus oldStatus = order.getOrderStatus();
        // nếu không được phép chuyển sang trạng thái mới thì throw exception
        if (!isAllowedTransition(oldStatus, targetStatus, hasCompletedPayment)) {
            throw new ValidationExceptionHandle(
                "Invalid order status transition from " + oldStatus + " to " + targetStatus,
                Collections.singletonList(targetStatus),
                "OrderModel"
            );
        }
        
        order.setOrderStatus(targetStatus);

        // nếu trạng thái mới là COMPLETED hoặc CANCELLED thì đặt thời gian hoàn thành
        if (!Objects.equals(oldStatus, targetStatus)) {
            if (targetStatus == OrderStatus.COMPLETED || targetStatus == OrderStatus.CANCELLED) {
                order.setCompletedAt(LocalDateTime.now());
            }
        }
    }

    /** Sau khi thanh toán đủ tiền thì đơn chuyển sang trạng thái PREPARING — không qua form staff. */
    public static boolean applyPreparingAfterFullPayment(OrderEntity order) {
        if (order == null || order.getOrderStatus() != OrderStatus.CONFIRMED) {
            return false;
        }
        order.setOrderStatus(OrderStatus.PREPARING);
        return true;
    }

    // kiểm tra xem trạng thái của order có được phép chuyển sang trạng thái mới không
    // oldStatus: trạng thái hiện tại của order
    // targetStatus: trạng thái mới của order
    // hasCompletedPayment: có thanh toán đủ chưa
    public static boolean isAllowedTransition(
        OrderStatus oldStatus,
        OrderStatus targetStatus,
        boolean hasCompletedPayment
    ) {
        if (oldStatus == null || targetStatus == null) {
            return false;
        }
        // nếu trạng thái hiện tại và trạng thái mới giống nhau thì được phép chuyển
        if (Objects.equals(oldStatus, targetStatus)) {
            return true;
        }
        // nếu trạng thái hiện tại là CANCELLED hoặc COMPLETED thì không được phép chuyển
        if (oldStatus == OrderStatus.CANCELLED || oldStatus == OrderStatus.COMPLETED) {
            return false;
        }
        // nếu trạng thái mới là CANCELLED thì không được phép chuyển nếu đã thanh toán
        if (targetStatus == OrderStatus.CANCELLED) {
            return !hasCompletedPayment;
        }
        // nếu trạng thái mới là COMPLETED thì không được phép chuyển nếu đơn không phải là PREPARING và đã thanh toán
        if (targetStatus == OrderStatus.COMPLETED) {
            return oldStatus == OrderStatus.PREPARING && hasCompletedPayment;
        }
        // nếu trạng thái hiện tại là CONFIRMED và trạng thái mới là PREPARING thì không được phép chuyển
        // vì phải thanh toán đủ mới chuyển sang PREPARING
        if (oldStatus == OrderStatus.CONFIRMED && targetStatus == OrderStatus.PREPARING) {
            return false;
        }
        int fromIdx = FORWARD_PIPELINE.indexOf(oldStatus);
        int toIdx = FORWARD_PIPELINE.indexOf(targetStatus);
        // là status completed hoặc cancelled không nằm trong pipeline thì không được phép chuyển
        if (fromIdx < 0 || toIdx < 0) {
            return false;
        }
        // phải chuyển từ trạng thái trước sang trạng thái sau trong pipeline
        return toIdx > fromIdx;
    }

    // lấy tất cả các trạng thái được phép chuyển sang từ trạng thái hiện tại
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

    // kiểm tra xem khách hàng có thể hủy đơn không
    public static boolean canCustomerCancel(OrderStatus current, boolean hasCompletedPayment) {
        if (current != OrderStatus.PENDING && current != OrderStatus.CONFIRMED) {
            return false;
        }
        return isAllowedTransition(current, OrderStatus.CANCELLED, hasCompletedPayment);
    }
}
