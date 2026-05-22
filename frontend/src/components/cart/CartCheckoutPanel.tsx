"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useCart } from "@/context/cart-context";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { OrderModel } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";

export function CartCheckoutPanel() {
  const { lines, total, clearCart } = useCart();
  const router = useRouter();
  const [notes, setNotes] = useState("");
  const [checkoutMsg, setCheckoutMsg] = useState<string | null>(null);
  const [checkoutPending, setCheckoutPending] = useState(false);

  async function checkout() {
    if (lines.length === 0) return;
    setCheckoutMsg(null);
    setCheckoutPending(true);
    try {
      const orderRes = await apiFetch<OrderModel>("/orders", {
        method: "POST",
        body: JSON.stringify({
          orderType: "DELIVERY",
          notes: notes.trim() || undefined,
        }),
      });
      const order = orderRes.data;
      const payloads = lines.map((line) => ({
        orderNumber: order.orderNumber,
        menuItemName: line.item.name,
        quantity: line.quantity,
      }));
      await apiFetch<unknown[]>("/order-items", {
        method: "POST",
        body: JSON.stringify(payloads),
      });
      await apiFetch<OrderModel>(`/orders/submit/${order.id}`, { method: "PATCH" });
      clearCart();
      setNotes("");
      router.push(`/checkout/payment?orderNumber=${encodeURIComponent(order.orderNumber)}`);
    } catch (e) {
      setCheckoutMsg(e instanceof ApiError ? e.message : "Đặt hàng thất bại");
    } finally {
      setCheckoutPending(false);
    }
  }

  return (
    <div className="space-y-4 rounded-2xl border border-stone-200 bg-surface p-5 shadow-sm lg:sticky lg:top-24">
      <h2
        className="font-serif text-xl font-semibold text-brand-900"
        style={{ fontFamily: "var(--font-cormorant), serif" }}
      >
        Thanh toán
      </h2>

      <p className="text-sm text-stone-600">
        {lines.length === 0 ? "Giỏ trống." : `${lines.length} món · `}
        <span className="font-semibold text-brand-800">{formatVnd(total)}</span>
      </p>

      <div className="space-y-2 border-t border-stone-100 pt-4">
        <p className="text-xs font-medium text-stone-600">Loại đơn</p>
        <p className="rounded-lg border border-stone-200 bg-stone-50 px-3 py-2 text-sm font-medium text-stone-800">
          Đặt hàng
        </p>
        <label className="block text-xs font-medium text-stone-600">Ghi chú</label>
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          rows={3}
          maxLength={300}
          className="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
          placeholder="Địa chỉ giao, số điện thoại nhận hàng…"
        />
      </div>

      {checkoutMsg ? (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800 ring-1 ring-red-100">{checkoutMsg}</p>
      ) : null}

      <button
        type="button"
        disabled={lines.length === 0 || checkoutPending}
        onClick={() => void checkout()}
        className="w-full rounded-xl bg-brand-800 py-3 text-sm font-semibold text-white shadow transition hover:bg-brand-900 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {checkoutPending ? "Đang đặt hàng…" : "Đặt hàng"}
      </button>

      <Link href="/menu" className="block text-center text-sm font-medium text-brand-800 hover:underline">
        ← Tiếp tục chọn món
      </Link>
    </div>
  );
}
