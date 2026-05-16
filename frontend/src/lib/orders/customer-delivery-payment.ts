import type { OrderModel } from "@/lib/api/types";
import { canCreatePaymentForOrder } from "@/lib/orders/order-payment";

/** Khách tự thanh toán VNPAY cho đơn giao hàng của mình (CONFIRMED + còn tiền). */
export function canCustomerPayDeliveryOrder(order: Pick<OrderModel, "orderType" | "orderStatus" | "totalAmount" | "totalOrderItem" | "canAcceptPayment">): boolean {
  return order.orderType === "DELIVERY" && canCreatePaymentForOrder(order);
}
