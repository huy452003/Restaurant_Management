"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect } from "react";
import { CustomerOnlinePaymentPanel } from "@/components/customer/CustomerOnlinePaymentPanel";
import { useAuth } from "@/context/auth-context";

function CheckoutPaymentContent() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderNumber = searchParams.get("orderNumber")?.trim() ?? "";

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      const next = orderNumber
        ? `/checkout/payment?orderNumber=${encodeURIComponent(orderNumber)}`
        : "/checkout/payment";
      router.replace(`/login?next=${encodeURIComponent(next)}`);
    }
  }, [user, authLoading, router, orderNumber]);

  if (authLoading || !user) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center text-muted">
        Đang kiểm tra phiên đăng nhập…
      </div>
    );
  }

  if (!orderNumber) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center">
        <p className="text-muted">Không có mã đơn để thanh toán.</p>
        <Link href="/orders" className="mt-4 inline-block text-sm font-medium text-brand-800 hover:underline">
          Xem đơn hàng
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-10 sm:px-6">
      <CustomerOnlinePaymentPanel orderNumber={orderNumber} />
    </div>
  );
}

export default function CheckoutPaymentPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[40vh] items-center justify-center text-muted">Đang tải…</div>
      }
    >
      <CheckoutPaymentContent />
    </Suspense>
  );
}
