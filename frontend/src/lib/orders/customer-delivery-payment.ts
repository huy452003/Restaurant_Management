import type { OrderModel, OrderStatus } from "@/lib/api/types";
import { canCreatePaymentForOrder, parsePositiveAmount } from "@/lib/orders/order-payment";

const TERMINAL: OrderStatus[] = ["COMPLETED", "CANCELLED"];

/** Khách thanh toán VNPAY cho đơn giao hàng (kể cả sau khi hủy cổng — PENDING/FAILED không chặn retry trên UI). */
export function canCustomerPayDeliveryOrder(
  order: Pick<OrderModel, "orderType" | "orderStatus" | "totalAmount" | "totalOrderItem" | "canAcceptPayment">,
): boolean {
  if (order.orderType !== "DELIVERY") return false;
  if (TERMINAL.includes(order.orderStatus)) return false;
  if (order.orderStatus === "PREPARING") return false;
  if (canCreatePaymentForOrder(order)) return true;
  if (order.orderStatus !== "CONFIRMED") return false;
  if ((order.totalOrderItem ?? 0) < 1) return false;
  return parsePositiveAmount(order.totalAmount) != null;
}
