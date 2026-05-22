"use client";

import Link from "next/link";
import { useAuth } from "@/context/auth-context";
import { btnPrimaryClass, btnSecondaryClass, fontSerif } from "@/lib/ui/bakery";

export function HomeHeroActions() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="flex flex-wrap gap-3">
        <div className="h-12 w-36 animate-pulse rounded-full bg-brand-100" />
        <div className="h-12 w-32 animate-pulse rounded-full bg-brand-50" />
      </div>
    );
  }

  return (
    <div className="flex flex-wrap gap-3 pt-1">
      <Link href="/menu" className={btnPrimaryClass}>
        {user ? "Vào thực đơn" : "Xem thực đơn"}
      </Link>
      <Link href={user ? "/reservations" : "/register"} className={btnSecondaryClass}>
        {user ? "Đặt bàn" : "Đăng ký ngay"}
      </Link>
    </div>
  );
}

export function HomeBottomCta() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-20 text-center sm:px-6">
        <div className="h-10 w-56 animate-pulse rounded-full bg-brand-100" />
        <div className="h-12 w-44 animate-pulse rounded-full bg-brand-200/60" />
      </div>
    );
  }

  if (user) {
    return (
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-20 text-center sm:px-6">
        <h2 className="font-serif text-3xl font-semibold text-brand-900 sm:text-4xl" style={fontSerif}>
          Chào {user.fullname?.split(" ")[0] ?? "bạn"}!
        </h2>
        <p className="max-w-md text-sm text-muted">Sẵn sàng đặt món hoặc giữ chỗ cho buổi tối hôm nay?</p>
        <div className="flex flex-wrap justify-center gap-3">
          <Link href="/menu" className={btnPrimaryClass}>
            Mở thực đơn
          </Link>
          <Link href="/reservations" className={btnSecondaryClass}>
            Đặt bàn
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col items-center gap-6 px-4 py-20 text-center sm:px-6">
      <h2 className="font-serif text-3xl font-semibold text-brand-900 sm:text-4xl" style={fontSerif}>
        Sẵn sàng trải nghiệm?
      </h2>
      <p className="max-w-md text-sm text-muted">Đăng ký miễn phí để đặt món, đặt bàn và theo dõi đơn của bạn.</p>
      <div className="flex flex-wrap justify-center gap-3">
        <Link href="/register" className={btnPrimaryClass}>
          Tạo tài khoản
        </Link>
        <Link href="/login" className={btnSecondaryClass}>
          Đăng nhập
        </Link>
      </div>
    </div>
  );
}
