"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { CartNavButton } from "@/components/CartNavButton";
import { LocaleSwitcher } from "@/components/LocaleSwitcher";
import { UserAccountMenu } from "@/components/UserAccountMenu";
import { useAuth } from "@/context/auth-context";
import type { UserRole } from "@/lib/api/types";
import { fontSerif } from "@/lib/ui/bakery";
import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";

type NavItem = {
  href: string;
  label: string;
  auth?: boolean;
  roles?: UserRole[];
};

const nav: NavItem[] = [
  { href: "/", label: "Trang chủ" },
  { href: "/about", label: "Về chúng tôi" },
  { href: "/menu", label: "Thực đơn", auth: true },
  { href: "/reservations", label: "Đặt bàn", auth: true },
];

export function SiteHeader() {
  const pathname = usePathname();
  const { user, loading } = useAuth();

  return (
    <header className="sticky top-0 z-50 border-b border-brand-100/90 bg-surface/90 shadow-[0_4px_24px_-8px_rgba(74,55,40,0.08)] backdrop-blur-md">
      <div className="mx-auto flex h-[4.25rem] max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
        <Link href="/" className="flex shrink-0 items-center gap-2.5">
          <span
            className="font-serif text-2xl font-semibold tracking-tight text-brand-900"
            style={fontSerif}
          >
            Bistro
          </span>
          <span className="hidden rounded-full bg-blush px-2.5 py-0.5 text-[0.65rem] font-semibold uppercase tracking-wide text-brand-800 sm:inline">
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
                className={`rounded-full px-4 py-2 text-sm font-medium transition-colors ${
                  active
                    ? "bg-brand-800 text-white shadow-sm"
                    : "text-muted hover:bg-brand-50 hover:text-brand-900"
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
                className="hidden max-w-[11rem] truncate rounded-full bg-brand-50 px-3 py-1 text-xs font-medium text-brand-800 sm:inline sm:max-w-[13rem]"
                title={STAFF_ROLE_LABEL_VI[user.role]}
              >
                {STAFF_ROLE_LABEL_VI[user.role]}
              </span>
              <CartNavButton />
              <UserAccountMenu />
            </>
          ) : !loading ? (
            <>
              <Link
                href="/login"
                className="rounded-full px-4 py-2 text-sm font-medium text-brand-800 transition hover:bg-brand-50"
              >
                Đăng nhập
              </Link>
              <Link
                href="/register"
                className="rounded-full bg-brand-800 px-5 py-2 text-sm font-semibold text-white shadow-md transition hover:bg-brand-900"
              >
                Đăng ký
              </Link>
            </>
          ) : null}
        </div>
      </div>

      <div className="flex gap-2 overflow-x-auto border-t border-brand-50 px-4 py-2.5 md:hidden">
        {nav.map((item) => {
          if (item.auth && !user) return null;
          if (item.roles && user && !item.roles.includes(user.role)) return null;
          const active = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`whitespace-nowrap rounded-full px-3.5 py-1.5 text-xs font-medium ${
                active ? "bg-brand-800 text-white" : "bg-brand-50 text-brand-800"
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
