"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback, useEffect, useId, useRef, useState } from "react";
import { useAuth } from "@/context/auth-context";
import type { UserRole } from "@/lib/api/types";
import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";

const STAFF_ROLES: UserRole[] = ["ADMIN", "MANAGER", "CASHIER"];

export function UserAccountMenu() {
  const { user, logout, hasRole } = useAuth();
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const menuId = useId();

  const close = useCallback(() => setOpen(false), []);

  useEffect(() => {
    close();
  }, [pathname, close]);

  useEffect(() => {
    if (!open) return;
    function onPointerDown(e: MouseEvent) {
      if (!rootRef.current?.contains(e.target as Node)) close();
    }
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") close();
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open, close]);

  if (!user) return null;

  const showAccount = user.role === "CUSTOMER";
  const showStaff = hasRole(...STAFF_ROLES);

  return (
    <div className="relative" ref={rootRef}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-controls={menuId}
        className="flex max-w-[14rem] items-center gap-1.5 rounded-lg border border-stone-200 bg-white px-2.5 py-1.5 text-sm font-medium text-stone-800 shadow-sm transition hover:bg-stone-50 sm:max-w-[18rem]"
        title={user.username}
      >
        <span className="truncate">{user.username}</span>
        <ChevronIcon open={open} />
      </button>

      {open ? (
        <div
          id={menuId}
          role="menu"
          className="absolute right-0 z-50 mt-1.5 min-w-[12.5rem] overflow-hidden rounded-xl border border-stone-200 bg-white py-1 shadow-lg ring-1 ring-stone-900/5"
        >
          <div className="border-b border-stone-100 px-3 py-2.5">
            <p className="truncate text-sm font-semibold text-stone-900">{user.username}</p>
            <p className="mt-0.5 text-xs text-stone-500">{STAFF_ROLE_LABEL_VI[user.role]}</p>
          </div>

          {showAccount ? (
            <>
              <MenuLink href="/orders" onSelect={close}>
                Đơn hàng
              </MenuLink>
              <MenuLink href="/account" onSelect={close}>
                Thông tin tài khoản
              </MenuLink>
              <MenuLink href="/account/password" onSelect={close}>
                Đổi mật khẩu
              </MenuLink>
            </>
          ) : null}

          {showStaff ? (
            <MenuLink href="/staff" onSelect={close}>
              Quản lý
            </MenuLink>
          ) : null}

          <div className="my-1 border-t border-stone-100" role="separator" />

          <button
            type="button"
            role="menuitem"
            onClick={() => {
              close();
              void logout();
            }}
            className="flex w-full px-3 py-2 text-left text-sm text-stone-700 transition hover:bg-stone-50"
          >
            Đăng xuất
          </button>
        </div>
      ) : null}
    </div>
  );
}

function MenuLink({
  href,
  children,
  onSelect,
}: {
  href: string;
  children: React.ReactNode;
  onSelect: () => void;
}) {
  return (
    <Link
      href={href}
      role="menuitem"
      onClick={onSelect}
      className="block px-3 py-2 text-sm font-medium text-stone-800 transition hover:bg-brand-50 hover:text-brand-900"
    >
      {children}
    </Link>
  );
}

function ChevronIcon({ open }: { open: boolean }) {
  return (
    <svg
      className={`h-4 w-4 shrink-0 text-stone-500 transition ${open ? "rotate-180" : ""}`}
      viewBox="0 0 20 20"
      fill="currentColor"
      aria-hidden
    >
      <path
        fillRule="evenodd"
        d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z"
        clipRule="evenodd"
      />
    </svg>
  );
}
