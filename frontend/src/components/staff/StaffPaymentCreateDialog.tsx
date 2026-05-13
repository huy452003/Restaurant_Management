"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { PaymentModel, VnpayCheckoutResponse } from "@/lib/api/types";

type Props = {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

const btnPrimary =
  "inline-flex min-h-[42px] flex-1 items-center justify-center rounded-lg border px-4 py-2.5 text-sm font-semibold transition disabled:opacity-50 sm:min-h-0";

export function StaffPaymentCreateDialog({ open, onClose, onSaved }: Props) {
  const [orderNumber, setOrderNumber] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pendingCash, setPendingCash] = useState(false);
  const [pendingVnpay, setPendingVnpay] = useState(false);

  useEffect(() => {
    if (!open) return;
    setError(null);
    setOrderNumber("");
    setPendingCash(false);
    setPendingVnpay(false);
  }, [open]);

  if (!open) return null;

  const pending = pendingCash || pendingVnpay;

  function validateOrderNumber(): string | null {
    const on = orderNumber.trim();
    if (on.length < 1 || on.length > 50) return "Mã đơn cần 1–50 ký tự";
    return null;
  }

  async function handleCash() {
    setError(null);
    const msg = validateOrderNumber();
    if (msg) {
      setError(msg);
      return;
    }
    const on = orderNumber.trim();
    setPendingCash(true);
    try {
      await apiFetch<PaymentModel>("/payments", {
        method: "POST",
        body: JSON.stringify({ orderNumber: on, paymentMethod: "CASH" }),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Tạo thanh toán tiền mặt thất bại");
    } finally {
      setPendingCash(false);
    }
  }

  async function handleVnpay() {
    setError(null);
    const msg = validateOrderNumber();
    if (msg) {
      setError(msg);
      return;
    }
    const on = orderNumber.trim();
    setPendingVnpay(true);
    try {
      const res = await apiFetch<VnpayCheckoutResponse>("/payments/vnpay/init", {
        method: "POST",
        body: JSON.stringify({ orderNumber: on }),
      });
      const url = res.data?.paymentUrl?.trim();
      if (!url) {
        setError("Không nhận được link thanh toán VNPAY");
        return;
      }
      const w = window.open(url, "_blank", "noopener,noreferrer");
      if (w == null) {
        window.location.assign(url);
        return;
      }
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Khởi tạo VNPAY thất bại");
    } finally {
      setPendingVnpay(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-stone-200 bg-surface p-6 shadow-xl"
        role="dialog"
        aria-labelledby="create-payment-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="create-payment-title" className="font-serif text-xl font-semibold text-brand-900">
          Thêm thanh toán
        </h2>

        <div className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          <label className="block text-xs font-medium text-stone-600">
            Mã đơn (orderNumber)
            <input
              className={fieldClass}
              value={orderNumber}
              onChange={(e) => setOrderNumber(e.target.value)}
              required
              minLength={1}
              maxLength={50}
              autoComplete="off"
            />
          </label>

          <div className="flex flex-col gap-2 pt-1 sm:flex-row">
            <button
              type="button"
              disabled={pending}
              onClick={() => void handleCash()}
              className={`${btnPrimary} border-brand-200 bg-brand-600 text-white hover:bg-brand-700`}
            >
              {pendingCash ? "Đang tạo…" : "Thanh toán tiền mặt"}
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => void handleVnpay()}
              className={`${btnPrimary} border-violet-300 bg-violet-600 text-white hover:bg-violet-700`}
            >
              {pendingVnpay ? "Đang mở VNPAY…" : "Thanh toán VNPAY"}
            </button>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={pending}
              className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium text-stone-700 hover:bg-stone-50 disabled:opacity-50"
            >
              Đóng
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
