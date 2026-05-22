import { ORDER_ITEM_STATUS_LABEL, ORDER_STATUS_LABEL } from "@/lib/orders/order-labels";
import { RESERVATION_STATUS_LABEL_VI } from "@/lib/reservations/labels";
import { TABLE_STATUS_LABEL_VI } from "@/lib/tables/table-labels";
import type { OrderStatus } from "@/lib/api/types";

/** Kiểu badge pill — đồng bộ với đặt bàn PENDING (amber). */
export type StatusBadgeVariant =
  | "pending"
  | "confirmed"
  | "preparing"
  | "success"
  | "danger"
  | "warning"
  | "reserved"
  | "neutral";

export const STATUS_BADGE_BASE =
  "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset";

export const STATUS_BADGE_VARIANT_CLASS: Record<StatusBadgeVariant, string> = {
  pending: "bg-amber-100 text-amber-900 ring-amber-200/80",
  confirmed: "bg-sky-100 text-sky-900 ring-sky-200/80",
  preparing: "bg-blue-100 text-blue-900 ring-blue-200/80",
  success: "bg-emerald-100 text-emerald-900 ring-emerald-200/80",
  danger: "bg-red-100 text-red-900 ring-red-200/80",
  warning: "bg-orange-100 text-orange-900 ring-orange-200/80",
  reserved: "bg-violet-100 text-violet-900 ring-violet-200/80",
  neutral: "bg-stone-100 text-stone-700 ring-stone-200/80",
};

export type StatusBadgeDomain =
  | "order"
  | "orderItem"
  | "payment"
  | "reservation"
  | "table"
  | "user"
  | "shift"
  | "category"
  | "menuItem";

const PAYMENT_STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ thanh toán",
  COMPLETED: "Đã thanh toán",
  FAILED: "Thất bại",
  CANCELLED: "Đã hủy",
};

const USER_STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Hoạt động",
  INACTIVE: "Ngưng",
  PENDING: "Chờ xác nhận",
};

const SHIFT_STATUS_LABEL: Record<string, string> = {
  SCHEDULED: "Đã đặt ca",
  IN_PROGRESS: "Đang diễn ra",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
};

const CATEGORY_STATUS_LABEL: Record<string, string> = {
  AVAILABLE: "Có sẵn",
  OUT_OF_STOCK: "Tạm hết hàng",
  DISCONTINUED: "Ngừng kinh doanh",
};

const MENU_ITEM_STATUS_LABEL: Record<string, string> = {
  AVAILABLE: "Có sẵn",
  OUT_OF_STOCK: "Tạm hết hàng",
  DISCONTINUED: "Ngừng kinh doanh",
};

const VARIANT_MAP: Record<StatusBadgeDomain, Record<string, StatusBadgeVariant>> = {
  order: {
    PENDING: "pending",
    CONFIRMED: "confirmed",
    PREPARING: "preparing",
    COMPLETED: "success",
    CANCELLED: "danger",
  },
  orderItem: {
    PENDING: "pending",
    CONFIRMED: "confirmed",
    PREPARING: "preparing",
    COMPLETED: "success",
    CANCELLED: "danger",
  },
  payment: {
    PENDING: "pending",
    COMPLETED: "success",
    FAILED: "danger",
    CANCELLED: "danger",
  },
  reservation: {
    PENDING: "pending",
    CONFIRMED: "confirmed",
    COMPLETED: "success",
    CANCELLED: "danger",
    NO_SHOW: "warning",
  },
  table: {
    AVAILABLE: "success",
    OCCUPIED: "pending",
    RESERVED: "reserved",
  },
  user: {
    ACTIVE: "success",
    INACTIVE: "neutral",
    PENDING: "pending",
  },
  shift: {
    SCHEDULED: "confirmed",
    IN_PROGRESS: "preparing",
    COMPLETED: "success",
    CANCELLED: "danger",
  },
  category: {
    AVAILABLE: "success",
    OUT_OF_STOCK: "warning",
    DISCONTINUED: "neutral",
  },
  menuItem: {
    AVAILABLE: "success",
    OUT_OF_STOCK: "warning",
    DISCONTINUED: "neutral",
  },
};

const LABEL_MAP: Record<StatusBadgeDomain, Record<string, string>> = {
  order: ORDER_STATUS_LABEL as unknown as Record<string, string>,
  orderItem: ORDER_ITEM_STATUS_LABEL,
  payment: PAYMENT_STATUS_LABEL,
  reservation: RESERVATION_STATUS_LABEL_VI,
  table: TABLE_STATUS_LABEL_VI,
  user: USER_STATUS_LABEL,
  shift: SHIFT_STATUS_LABEL,
  category: CATEGORY_STATUS_LABEL,
  menuItem: MENU_ITEM_STATUS_LABEL,
};

export function statusBadgeVariant(
  domain: StatusBadgeDomain,
  status: string | undefined,
): StatusBadgeVariant {
  const key = status?.toUpperCase() ?? "";
  return VARIANT_MAP[domain][key] ?? "neutral";
}

export function statusBadgeLabel(
  domain: StatusBadgeDomain,
  status: string | undefined,
): string {
  if (!status?.trim()) return "—";
  const key = status.toUpperCase();
  return LABEL_MAP[domain][key] ?? status;
}

export function statusBadgeClassName(
  domain: StatusBadgeDomain,
  status: string | undefined,
  extra?: string,
): string {
  const variant = statusBadgeVariant(domain, status);
  return [STATUS_BADGE_BASE, STATUS_BADGE_VARIANT_CLASS[variant], extra].filter(Boolean).join(" ");
}

/** Gõ order status an toàn cho domain order. */
export function orderStatusBadgeProps(status: OrderStatus) {
  return {
    domain: "order" as const,
    status,
    label: ORDER_STATUS_LABEL[status],
  };
}
