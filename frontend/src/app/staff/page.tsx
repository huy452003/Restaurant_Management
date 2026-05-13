"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo } from "react";
import { useAuth } from "@/context/auth-context";
import { STAFF_CARDS, staffCardVisible } from "@/lib/staff/staff-cards";

export default function StaffHomePage() {
  const { user, loading, hasRole } = useAuth();
  const router = useRouter();

  const cards = useMemo(() => {
    if (!user) return [];
    return STAFF_CARDS.filter((c) => staffCardVisible(user.role, c));
  }, [user]);

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace("/login?next=/staff");
      return;
    }
    if (!hasRole("ADMIN", "MANAGER", "CASHIER", "WAITER", "CHEF")) {
      router.replace("/menu");
    }
  }, [user, loading, hasRole, router]);

  if (loading || !user) {
    return <div className="py-20 text-center text-muted">Đang tải…</div>;
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
      <h1
        className="font-serif text-3xl font-semibold text-brand-900"
        style={{ fontFamily: "var(--font-cormorant), serif" }}
      >
        Khu vực quản lý
      </h1>

      <ul className="mt-10 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {cards.map((c) => (
          <li key={c.href}>
            <Link
              href={c.href}
              className="flex h-full flex-col rounded-2xl border border-stone-200 bg-surface p-6 shadow-sm transition hover:border-brand-200 hover:shadow-md"
            >
              <h2
                className="font-serif text-xl font-semibold text-brand-900"
                style={{ fontFamily: "var(--font-cormorant), serif" }}
              >
                {c.title}
              </h2>
              <p className="mt-2 flex-1 text-sm leading-relaxed text-muted">{c.desc}</p>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
