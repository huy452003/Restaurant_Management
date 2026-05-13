import type { UserRole } from "@/lib/api/types";

/** Nhãn hiển thị (không dùng mã enum tiếng Anh trên UI). */
export const STAFF_ROLE_LABEL_VI: Record<UserRole, string> = {
  ADMIN: "Quản trị hệ thống",
  CUSTOMER: "Khách hàng",
  MANAGER: "Quản lý vận hành",
  WAITER: "Phục vụ",
  CHEF: "Bếp",
  CASHIER: "Thu ngân",
};
