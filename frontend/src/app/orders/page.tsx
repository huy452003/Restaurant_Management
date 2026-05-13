"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { OrderModel, PaginatedResponse } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";

export default function OrdersPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [orders, setOrders] = useState<OrderModel[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const path = hasRole("CUSTOMER")
        ? `/orders/filters?${buildPageParams(0, 20)}`
        : `/orders/filters/admin?${buildPageParams(0, 20)}`;
      const res = await apiFetch<PaginatedResponse<OrderModel>>(path);
      setOrders(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được đơn");
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, [user, hasRole]);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/orders");
      return;
    }
    void Promise.resolve().then(() => load());
  }, [user, authLoading, router, load]);

  async function submitOrder(id: number) {
    setBusyId(id);
    setError(null);
    try {
      await apiFetch<OrderModel>(`/orders/submit/${id}`, { method: "PATCH" });
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Gửi đơn thất bại");
    } finally {
      setBusyId(null);
    }
  }

  async function cancelOrder(id: number) {
    setBusyId(id);
    setError(null);
    try {
      await apiFetch<OrderModel>(`/orders/cancel/${id}`, { method: "PATCH" });
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Hủy thất bại");
    } finally {
      setBusyId(null);
    }
  }

  if (authLoading || !user) {
    return <div className="py-20 text-center text-muted">Đang tải…</div>;
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1
            className="font-serif text-3xl font-semibold text-brand-900"
            style={{ fontFamily: "var(--font-cormorant), serif" }}
          >
            Đơn hàng
          </h1>
          <p className="mt-1 text-sm text-muted">
            {hasRole("CUSTOMER") ? "Đơn của bạn — gửi bếp khi sẵn sàng." : "Theo dõi toàn bộ đơn trong hệ thống."}
          </p>
        </div>
        <Link
          href="/menu"
          className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium text-stone-800 hover:bg-stone-50"
        >
          Thêm món
        </Link>
      </div>

      {error ? <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

      {loading ? (
        <p className="mt-8 text-muted">Đang tải đơn…</p>
      ) : orders.length === 0 ? (
        <p className="mt-8 rounded-xl border border-dashed border-stone-200 bg-stone-50 p-8 text-center text-muted">
          Chưa có đơn.{" "}
          <Link href="/menu" className="font-medium text-brand-800 underline">
            Đặt món
          </Link>
        </p>
      ) : (
        <ul className="mt-8 space-y-4">
          {orders.map((o) => (
            <li
              key={o.id}
              className="rounded-2xl border border-stone-200 bg-surface p-5 shadow-sm"
            >
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-mono text-sm font-semibold text-brand-900">{o.orderNumber}</p>
                  <p className="mt-1 text-sm text-muted">
                    Bàn {o.tableNumber} · {o.orderType} ·{" "}
                    <span className="font-medium text-stone-800">{o.orderStatus}</span>
                  </p>
                  {o.totalAmount != null ? (
                    <p className="mt-2 text-lg font-semibold text-brand-800">{formatVnd(o.totalAmount)}</p>
                  ) : null}
                </div>
                {hasRole("CUSTOMER") ? (
                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      disabled={busyId === o.id}
                      onClick={() => void submitOrder(o.id)}
                      className="rounded-lg bg-brand-800 px-3 py-2 text-sm font-medium text-white hover:bg-brand-900 disabled:opacity-50"
                    >
                      Gửi bếp
                    </button>
                    <button
                      type="button"
                      disabled={busyId === o.id}
                      onClick={() => void cancelOrder(o.id)}
                      className="rounded-lg border border-red-200 px-3 py-2 text-sm font-medium text-red-800 hover:bg-red-50 disabled:opacity-50"
                    >
                      Hủy đơn
                    </button>
                  </div>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
