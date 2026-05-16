import type { OrderModel, OrderStatus } from "@/lib/api/types";

const FALLBACK_STATUSES: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "PREPARING",
  "COMPLETED",
  "CANCELLED",
];

/** Trạng thái staff được chọn — ưu tiên danh sách từ backend. */
export function selectableOrderStatusesForStaff(
  order: Pick<OrderModel, "orderStatus" | "allowedOrderStatuses">,
): OrderStatus[] {
  if (order.allowedOrderStatuses && order.allowedOrderStatuses.length > 0) {
    return order.allowedOrderStatuses;
  }
  return FALLBACK_STATUSES.filter((s) => s === order.orderStatus || s !== "CANCELLED");
}
