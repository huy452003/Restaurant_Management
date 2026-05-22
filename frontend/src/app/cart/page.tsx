"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { CartCheckoutPanel } from "@/components/cart/CartCheckoutPanel";
import { CartItemsPanel } from "@/components/cart/CartItemsPanel";
import { PageHeading } from "@/components/ui/PageHeading";
import { useAuth } from "@/context/auth-context";

export default function CartPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/cart");
    }
  }, [user, authLoading, router]);

  if (authLoading || !user) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-muted">
        Đang kiểm tra phiên đăng nhập…
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <PageHeading title="Giỏ hàng" subtitle="Xem lại món đã chọn và tiến hành thanh toán." />

      <div className="mt-8 grid gap-8 lg:grid-cols-[minmax(280px,340px)_1fr] lg:items-start">
        <CartCheckoutPanel />
        <div>
          <CartItemsPanel />
        </div>
      </div>
    </div>
  );
}
