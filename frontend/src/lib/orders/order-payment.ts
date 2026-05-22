import type { OrderModel } from "@/lib/api/types";

type OrderPaymentFields = Pick<
  OrderModel,
  "orderStatus" | "totalAmount" | "totalOrderItem" | "canAcceptPayment"
>;

/** API có thể trả totalAmount dạng string hoặc number (Jackson). */
export function parsePositiveAmount(totalAmount?: string | number | null): number | null {
  if (totalAmount == null) return null;
  if (typeof totalAmount === "number") {
    return Number.isFinite(totalAmount) && totalAmount > 0 ? totalAmount : null;
  }
  const s = String(totalAmount).trim();
  if (s === "") return null;
  const n = Number(s);
  if (!Number.isFinite(n) || n <= 0) return null;
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
