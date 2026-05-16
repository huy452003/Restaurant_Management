import type { OrderStatus, OrderType } from "@/lib/api/types";

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang chuẩn bị",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

export const ORDER_TYPE_LABEL: Record<OrderType, string> = {
  DINE_IN: "Tại chỗ",
  DELIVERY: "Giao hàng",
};

export const ORDER_ITEM_STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang làm",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};
