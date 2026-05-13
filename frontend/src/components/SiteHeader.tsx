"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LocaleSwitcher } from "@/components/LocaleSwitcher";
import { useAuth } from "@/context/auth-context";
import type { UserRole } from "@/lib/api/types";
import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";

type NavItem = {
  href: string;
  label: string;
  auth?: boolean;
  roles?: UserRole[];
};

const nav: NavItem[] = [
  { href: "/", label: "Trang chủ" },
  { href: "/menu", label: "Thực đơn", auth: true },
  { href: "/orders", label: "Đơn hàng", auth: true, roles: ["CUSTOMER"] },
  { href: "/reservations", label: "Đặt bàn", auth: true, roles: ["CUSTOMER"] },
  {
    href: "/staff",
    label: "Quản lý",
    auth: true,
    roles: ["ADMIN", "MANAGER", "CASHIER", "WAITER", "CHEF"],
  },
];

export function SiteHeader() {
  const pathname = usePathname();
  const { user, loading, logout } = useAuth();

  return (
    <header className="sticky top-0 z-50 border-b border-stone-200/80 bg-surface/95 shadow-sm backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2 shrink-0">
          <span
            className="font-serif text-2xl font-semibold tracking-tight text-brand-800"
            style={{ fontFamily: "var(--font-cormorant), serif" }}
          >
            Bistro
          </span>
          <span className="hidden rounded-full bg-brand-100 px-2 py-0.5 text-xs font-medium text-brand-800 sm:inline">
            Nhà hàng
          </span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          {nav.map((item) => {
            if (item.auth && !user) return null;
            if (item.roles && user && !item.roles.includes(user.role)) return null;
            const active = pathname === item.href || pathname.startsWith(item.href + "/");
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  active
                    ? "bg-brand-100 text-brand-900"
                    : "text-stone-600 hover:bg-stone-100 hover:text-stone-900"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex flex-wrap items-center justify-end gap-x-3 gap-y-2 sm:gap-x-4">
          <LocaleSwitcher />
          {!loading && user ? (
            <>
              <span
                className="max-w-[11rem] truncate rounded-md bg-stone-100 px-2 py-1 text-xs font-medium text-stone-700 sm:max-w-[13rem]"
                title={STAFF_ROLE_LABEL_VI[user.role]}
              >
                {STAFF_ROLE_LABEL_VI[user.role]}
              </span>
              <span
                className="max-w-[10rem] truncate text-sm font-medium text-stone-800 sm:max-w-[14rem] md:max-w-[18rem]"
                title={user.username}
              >
                {user.username}
              </span>
              <button
                type="button"
                onClick={() => void logout()}
                className="rounded-lg border border-stone-200 px-3 py-2 text-sm font-medium text-stone-700 transition hover:bg-stone-50"
              >
                Đăng xuất
              </button>
            </>
          ) : !loading ? (
            <>
              <Link
                href="/login"
                className="rounded-lg px-3 py-2 text-sm font-medium text-stone-700 hover:bg-stone-100"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                className="rounded-lg bg-brand-800 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-900"
              >
                Đăng ký
              </Link>
            </>
          ) : null}
        </div>
      </div>

      <div className="flex gap-1 overflow-x-auto border-t border-stone-100 px-4 py-2 md:hidden">
        {nav.map((item) => {
          if (item.auth && !user) return null;
          if (item.roles && user && !item.roles.includes(user.role)) return null;
          const active = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`whitespace-nowrap rounded-full px-3 py-1.5 text-xs font-medium ${
                active ? "bg-brand-800 text-white" : "bg-stone-100 text-stone-700"
              }`}
            >
              {item.label}
            </Link>
          );
        })}
      </div>
    </header>
  );
}
