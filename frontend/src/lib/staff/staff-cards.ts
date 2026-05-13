import type { UserRole } from "@/lib/api/types";

export type StaffCardConfig = {
  href: string;
  title: string;
  desc: string;
  /** Hiển thị khi người dùng có ít nhất một trong các vai trò này */
  accessRoles: UserRole[];
};

/** Thứ tự hiển thị trên dashboard /staff */
export const STAFF_CARDS: StaffCardConfig[] = [
  {
    href: "/staff/users",
    title: "Người dùng",
    desc: "Danh sách, lọc và phân trang người dùng.",
    accessRoles: ["ADMIN"],
  },
  {
    href: "/staff/orders",
    title: "Đơn hàng",
    desc: "Toàn bộ đơn hàng trong hệ thống.",
    accessRoles: ["ADMIN", "MANAGER"],
  },
  {
    href: "/staff/reservations",
    title: "Đặt chỗ",
    desc: "Đặt bàn và lịch đặt chỗ của khách.",
    accessRoles: ["ADMIN", "MANAGER"],
  },
  {
    href: "/staff/categories",
    title: "Danh mục",
    desc: "Nhóm món, danh mục thực đơn.",
    accessRoles: ["ADMIN", "MANAGER"],
  },
  {
    href: "/staff/menu-items",
    title: "Món ăn",
    desc: "Thực đơn và món trong hệ thống.",
    accessRoles: ["ADMIN", "MANAGER"],
  },
  {
    href: "/staff/tables",
    title: "Bàn",
    desc: "Sơ đồ bàn và trạng thái chỗ.",
    accessRoles: ["ADMIN", "MANAGER", "WAITER", "CHEF", "CASHIER"],
  },
  {
    href: "/staff/shifts",
    title: "Ca làm",
    desc: "Lịch ca làm nhân viên.",
    accessRoles: ["ADMIN", "MANAGER", "WAITER", "CHEF", "CASHIER"],
  },
  {
    href: "/staff/payments",
    title: "Thanh toán",
    desc: "Giao dịch và thanh toán.",
    accessRoles: ["ADMIN", "MANAGER", "CASHIER"],
  },
];

export function staffCardVisible(role: UserRole, card: StaffCardConfig): boolean {
  return card.accessRoles.includes(role);
}
