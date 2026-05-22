"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCartOptional } from "@/context/cart-context";

export function CartNavButton() {
  const pathname = usePathname();
  const cart = useCartOptional();
  const count = cart?.itemCount ?? 0;
  const active = pathname === "/cart" || pathname.startsWith("/cart/");

  return (
    <Link
      href="/cart"
      className={`relative flex h-10 w-10 items-center justify-center rounded-full border transition ${
        active
          ? "border-brand-300 bg-brand-800 text-white"
          : "border-brand-100 bg-brand-50 text-brand-800 hover:bg-blush"
      }`}
      aria-label={count > 0 ? `Giỏ hàng, ${count} món` : "Giỏ hàng"}
      title="Giỏ hàng"
    >
      <CartIcon className="h-5 w-5" />
      {count > 0 ? (
        <span className="absolute -right-1 -top-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-800 px-1 text-[10px] font-bold text-white">
          {count > 99 ? "99+" : count}
        </span>
      ) : null}
    </Link>
  );
}

function CartIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden>
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M6 6h15l-1.5 9h-12L6 6zm0 0L5 3H2M9 20a1 1 0 100-2 1 1 0 000 2zm8 0a1 1 0 100-2 1 1 0 000 2z"
      />
    </svg>
  );
}
