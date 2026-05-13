"use client";

import Link from "next/link";
import { useAuth } from "@/context/auth-context";

export function HomeHeroActions() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex flex-wrap gap-3">
        <div className="h-12 w-40 animate-pulse rounded-xl bg-white/10" />
      </div>
    );
  }

  return (
    <div className="flex flex-wrap gap-3">
      <Link
        href="/menu"
        className="inline-flex items-center justify-center rounded-xl bg-amber-400 px-6 py-3 text-base font-semibold text-stone-900 shadow-lg transition hover:bg-amber-300"
      >
        {user ? "Vào thực đơn" : "Xem thực đơn"}
      </Link>
    </div>
  );
}

export function HomeBottomCta() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-16 text-center sm:px-6">
        <div className="h-10 w-56 animate-pulse rounded-lg bg-stone-200/90" />
        <div className="h-4 max-w-lg w-full animate-pulse rounded bg-stone-200/70" />
        <div className="h-12 w-44 animate-pulse rounded-xl bg-amber-200/80" />
      </div>
    );
  }

  if (user) {
    return (
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-16 text-center sm:px-6">
        <h2
          className="font-serif text-3xl font-semibold text-brand-900 sm:text-4xl"
          style={{ fontFamily: "var(--font-cormorant), serif" }}
        >
          Chào {user.fullname?.split(" ")[0] ?? "bạn"}
        </h2>
        <Link
          href="/menu"
          className="rounded-xl bg-amber-400 px-8 py-3 text-base font-semibold text-stone-900 shadow-md transition hover:bg-amber-300"
        >
          Mở thực đơn
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-16 text-center sm:px-6">
      <h2
        className="font-serif text-3xl font-semibold text-brand-900 sm:text-4xl"
        style={{ fontFamily: "var(--font-cormorant), serif" }}
      >
        Sẵn sàng đặt món?
      </h2>
      <p className="max-w-lg text-muted">
        Xem thực đơn theo dữ liệu thật từ server (cần đăng nhập để gọi API). Proxy qua{" "}
        <code className="rounded bg-stone-200 px-1.5 py-0.5 text-xs text-stone-800">/api/proxy</code>{" "}
        khi dev.
      </p>
      <Link
        href="/menu"
        className="rounded-xl bg-amber-400 px-8 py-3 text-base font-semibold text-stone-900 shadow-md transition hover:bg-amber-300"
      >
        Xem thực đơn
      </Link>
    </div>
  );
}
