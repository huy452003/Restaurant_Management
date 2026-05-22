import type { FilterField } from "@/components/list/FilterBar";
import { PAGE_SORT_ID_DESC } from "@/lib/api/client";

/** Truyền vào `usePaginatedList({ extraParams: LIST_EXTRA_SORT_NEWEST })` — khớp API orders/payments/reservations/shifts. */
export const LIST_EXTRA_SORT_NEWEST = { sort: PAGE_SORT_ID_DESC } as const;
import { ORDER_STATUS_LABEL, ORDER_TYPE_LABEL } from "@/lib/orders/order-labels";
import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";

const empty = "— Tất cả —";

function enumSelect<T extends string>(labels: Record<T, string>): { value: string; label: string }[] {
  return (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }));
}

export const USER_LIST_FILTERS: FilterField[] = [
  { key: "username", label: "Tên đăng nhập", type: "text", placeholder: "Tìm theo username…" },
  { key: "fullname", label: "Họ tên", type: "text" },
  { key: "email", label: "Email", type: "text" },
  { key: "phone", label: "SĐT", type: "text" },
  {
    key: "role",
    label: "Vai trò",
    type: "select",
    emptyOption: empty,
    options: enumSelect(STAFF_ROLE_LABEL_VI),
  },
  {
    key: "userStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "ACTIVE", label: "ACTIVE" },
      { value: "INACTIVE", label: "INACTIVE" },
      { value: "PENDING", label: "PENDING" },
    ],
  },
];

export const CATEGORY_LIST_FILTERS: FilterField[] = [
  { key: "name", label: "Tên danh mục", type: "text" },
  {
    key: "categoryStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "AVAILABLE", label: "Có sẵn" },
      { value: "OUT_OF_STOCK", label: "Tạm hết hàng" },
      { value: "DISCONTINUED", label: "Ngừng kinh doanh" },
    ],
  },
];

export const MENU_ITEM_LIST_FILTERS: FilterField[] = [
  { key: "name", label: "Tên món", type: "text" },
  { key: "categoryName", label: "Danh mục", type: "text" },
  {
    key: "menuItemStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "AVAILABLE", label: "AVAILABLE" },
      { value: "OUT_OF_STOCK", label: "OUT_OF_STOCK" },
      { value: "DISCONTINUED", label: "DISCONTINUED" },
    ],
  },
];

export const TABLE_LIST_FILTERS: FilterField[] = [
  { key: "tableNumber", label: "Số bàn", type: "number", placeholder: "VD: 5" },
  { key: "location", label: "Vị trí", type: "text" },
  {
    key: "tableStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "AVAILABLE", label: "Trống" },
      { value: "OCCUPIED", label: "Đang phục vụ" },
      { value: "RESERVED", label: "Đã đặt" },
    ],
  },
];

export const ORDER_LIST_FILTERS: FilterField[] = [
  { key: "orderNumber", label: "Mã đơn", type: "text" },
  { key: "tableNumber", label: "Số bàn", type: "number" },
  {
    key: "orderStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: enumSelect(ORDER_STATUS_LABEL),
  },
  {
    key: "orderType",
    label: "Loại đơn",
    type: "select",
    emptyOption: empty,
    options: enumSelect(ORDER_TYPE_LABEL),
  },
];

export const ORDER_ADMIN_LIST_FILTERS: FilterField[] = [
  ...ORDER_LIST_FILTERS,
  { key: "customerName", label: "Tên khách", type: "text" },
  { key: "customerPhone", label: "SĐT khách", type: "text" },
];

export const PAYMENT_LIST_FILTERS: FilterField[] = [
  { key: "orderNumber", label: "Mã đơn", type: "text" },
  { key: "cashierFullname", label: "Thu ngân", type: "text" },
  {
    key: "paymentMethod",
    label: "Phương thức",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "CASH", label: "Tiền mặt" },
      { value: "VNPAY", label: "VNPAY" },
    ],
  },
  {
    key: "paymentStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "PENDING", label: "Chờ thanh toán" },
      { value: "COMPLETED", label: "Đã thanh toán" },
      { value: "FAILED", label: "Thất bại" },
      { value: "CANCELLED", label: "Đã hủy" },
    ],
  },
];

export const RESERVATION_LIST_FILTERS: FilterField[] = [
  { key: "tableNumber", label: "Số bàn", type: "number" },
  {
    key: "reservationStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "PENDING", label: "PENDING" },
      { value: "CONFIRMED", label: "CONFIRMED" },
      { value: "CANCELLED", label: "CANCELLED" },
      { value: "COMPLETED", label: "COMPLETED" },
    ],
  },
];

export const RESERVATION_ADMIN_LIST_FILTERS: FilterField[] = [
  ...RESERVATION_LIST_FILTERS,
  { key: "customerName", label: "Tên khách", type: "text" },
  { key: "customerPhone", label: "SĐT", type: "text" },
];

export const SHIFT_LIST_FILTERS: FilterField[] = [
  { key: "shiftDate", label: "Ngày ca (yyyy-MM-dd)", type: "text", placeholder: "2026-05-17" },
  {
    key: "shiftStatus",
    label: "Trạng thái",
    type: "select",
    emptyOption: empty,
    options: [
      { value: "SCHEDULED", label: "Đã đặt ca" },
      { value: "IN_PROGRESS", label: "Đang diễn ra" },
      { value: "COMPLETED", label: "Đã hoàn thành" },
      { value: "CANCELLED", label: "Đã hủy" },
    ],
  },
];
