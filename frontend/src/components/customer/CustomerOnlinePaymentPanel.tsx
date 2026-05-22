"use client";

import Link from "next/link";
import { useState } from "react";
import { initVnpayCheckout } from "@/lib/payments/init-vnpay";

const MOMO_LOCKED_MSG = "Chức năng đang tạm khóa";

const payBtnClass =
  "inline-flex min-h-[44px] w-full items-center justify-center rounded-lg border px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50";

type Props = {
  orderNumber: string;
};

export function CustomerOnlinePaymentPanel({ orderNumber }: Props) {
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [momoLockedMsg, setMomoLockedMsg] = useState<string | null>(null);
  const [pendingVnpay, setPendingVnpay] = useState(false);

  async function handleVnpay() {
    setPaymentError(null);
    setMomoLockedMsg(null);
    setPendingVnpay(true);
    const result = await initVnpayCheckout(orderNumber);
    if (!result.ok) {
      setPaymentError(result.message);
    }
    setPendingVnpay(false);
  }

  function handleMomo() {
    setPaymentError(null);
    setMomoLockedMsg(MOMO_LOCKED_MSG);
  }

  return (
    <div className="space-y-4 rounded-2xl border border-stone-200 bg-surface p-6 shadow-sm">
      <div>
        <h1
          className="font-serif text-2xl font-semibold text-brand-900"
          style={{ fontFamily: "var(--font-cormorant), serif" }}
        >
          Thanh toán online
        </h1>
        <p className="mt-1 text-sm text-muted">
          Đơn <span className="font-mono font-semibold text-brand-900">{orderNumber}</span> đã được xác nhận.
          Chọn phương thức thanh toán.
        </p>
      </div>

      <div className="space-y-2">
        <button
          type="button"
          disabled={pendingVnpay}
          onClick={() => void handleVnpay()}
          className={`${payBtnClass} border-violet-300 bg-violet-600 text-white hover:bg-violet-700`}
        >
          {pendingVnpay ? "Đang mở VNPAY…" : "Thanh toán VNPAY"}
        </button>
        <button
          type="button"
          onClick={handleMomo}
          className={`${payBtnClass} border-pink-300 bg-pink-600 text-white hover:bg-pink-700`}
        >
          Thanh toán MoMo
        </button>
        {momoLockedMsg ? <p className="text-sm text-red-700">{momoLockedMsg}</p> : null}
        {paymentError ? (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800 ring-1 ring-red-100">
            {paymentError}
          </p>
        ) : null}
      </div>

      <Link href="/orders" className="block text-center text-sm font-medium text-brand-800 hover:underline">
        Xem đơn hàng của tôi
      </Link>
    </div>
  );
}
