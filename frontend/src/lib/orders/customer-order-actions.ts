import type { OrderModel, OrderStatus } from "@/lib/api/types";

/** Khớp backend: chỉ submit đơn PENDING. */
export function canCustomerSubmitOrder(status: OrderStatus): boolean {
  return status === "PENDING";
}

type OrderCancelFields = Pick<OrderModel, "orderStatus" | "allowedOrderStatuses">;

function isTerminalOrderStatus(status: OrderStatus): boolean {
  return status === "CANCELLED" || status === "COMPLETED";
}

/** Khớp backend: được phép chuyển sang CANCELLED (không phải đã ở trạng thái cuối). */
export function canCustomerCancelOrder(order: OrderCancelFields): boolean {
  if (isTerminalOrderStatus(order.orderStatus)) {
    return false;
  }
  if (order.allowedOrderStatuses?.length) {
    return order.allowedOrderStatuses.some(
      (s) => s === "CANCELLED" && s !== order.orderStatus,
    );
  }
  return order.orderStatus === "PENDING" || order.orderStatus === "CONFIRMED";
}

/** Khớp backend: updateForCustomer + thêm/sửa món khi PENDING. */
export function canCustomerEditOrder(status: OrderStatus): boolean {
  return status === "PENDING";
}
