import type { OrderModel } from "@/lib/api/types";

type OrderPaymentFields = Pick<
  OrderModel,
  "orderStatus" | "totalAmount" | "totalOrderItem" | "canAcceptPayment"
>;

function parsePositiveAmount(totalAmount?: string): number | null {
  if (totalAmount == null || totalAmount.trim() === "") {
    return null;
  }
  const n = Number(totalAmount);
  if (!Number.isFinite(n) || n <= 0) {
    return null;
  }
  return n;
}

/** Fallback khi API/cache chưa có canAcceptPayment — khớp PaymentServiceImp.canAcceptNewPayment (trừ allocated). */
function canCreatePaymentFallback(order: OrderPaymentFields): boolean {
  const { orderStatus, totalAmount, totalOrderItem } = order;
  if (orderStatus !== "CONFIRMED") {
    return false;
  }
  if ((totalOrderItem ?? 0) < 1) {
    return false;
  }
  return parsePositiveAmount(totalAmount) != null;
}

/** Đơn sẵn sàng tạo thanh toán mới (dùng cờ từ backend khi có). */
export function canCreatePaymentForOrder(order: OrderPaymentFields): boolean {
  if (order.canAcceptPayment === true) {
    return true;
  }
  if (order.canAcceptPayment === false) {
    return false;
  }
  return canCreatePaymentFallback(order);
}
