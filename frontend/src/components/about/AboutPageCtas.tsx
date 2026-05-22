"use client";

import Link from "next/link";
import { useAuth } from "@/context/auth-context";
import { btnPrimaryClass, btnSecondaryClass } from "@/lib/ui/bakery";

type Props = {
  className?: string;
};

export function AboutPageCtas({ className = "" }: Props) {
  const { user } = useAuth();

  return (
    <div className={`flex flex-wrap gap-3 ${className}`}>
      <Link href={user ? "/reservations" : "/register"} className={btnPrimaryClass}>
        {user ? "Đặt bàn ngay" : "Đăng ký & đặt bàn"}
      </Link>
      <Link href={user ? "/menu" : "/login"} className={btnSecondaryClass}>
        {user ? "Thực đơn" : "Đăng nhập"}
      </Link>
    </div>
  );
}
