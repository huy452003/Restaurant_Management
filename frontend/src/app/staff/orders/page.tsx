"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffOrderEditDialog } from "@/components/staff/StaffOrderEditDialog";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { OrderModel, OrderStatus, PaginatedResponse, TableModel, UserModel } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang chuẩn bị",
  READY: "Sẵn sàng",
  SERVED: "Đã phục vụ",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

function isTerminal(s: OrderStatus): boolean {
  return s === "COMPLETED" || s === "CANCELLED";
}

export default function StaffOrdersPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [orders, setOrders] = useState<OrderModel[]>([]);
  const [waiters, setWaiters] = useState<UserModel[]>([]);
  const [tables, setTables] = useState<TableModel[]>([]);
  const [editing, setEditing] = useState<OrderModel | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<OrderModel>>(
        `/orders/filters/admin?${buildPageParams(0, 30)}`,
      );
      setOrders(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không có quyền hoặc lỗi mạng");
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadWaiters = useCallback(async () => {
    try {
      const res = await apiFetch<PaginatedResponse<UserModel>>(
        `/users/filterAndPaginate?${buildPageParams(0, 100, { role: "WAITER" })}`,
      );
      setWaiters(res.data.content ?? []);
    } catch {
      setWaiters([]);
    }
  }, []);

  const loadTables = useCallback(async () => {
    try {
      const res = await apiFetch<PaginatedResponse<TableModel>>(
        `/tables/filters?${buildPageParams(0, 200)}`,
      );
      setTables(res.data.content ?? []);
    } catch {
      setTables([]);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/orders");
      return;
    }
    if (!hasRole("ADMIN", "MANAGER")) {
      router.replace("/staff");
      return;
    }
    void load();
    void loadWaiters();
    void loadTables();
  }, [user, authLoading, hasRole, router, load, loadWaiters, loadTables]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Đơn hàng
      </h1>

      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl border border-stone-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-stone-100 text-stone-600">
              <tr>
                <th className="px-4 py-3 font-medium">Mã đơn</th>
                <th className="px-4 py-3 font-medium">Bàn</th>
                <th className="px-4 py-3 font-medium">Khách</th>
                <th className="px-4 py-3 font-medium">Trạng thái</th>
                <th className="px-4 py-3 font-medium">Món</th>
                <th className="px-4 py-3 font-medium">Tổng</th>
                <th className="px-4 py-3 font-medium w-28">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => {
                const terminal = isTerminal(o.orderStatus);
                return (
                  <tr key={o.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                    <td className="px-4 py-3 font-mono text-xs">{o.orderNumber}</td>
                    <td className="px-4 py-3">{o.tableNumber}</td>
                    <td className="px-4 py-3 text-muted">{o.customerName ?? "—"}</td>
                    <td className="px-4 py-3">{STATUS_LABEL[o.orderStatus]}</td>
                    <td className="px-4 py-3 tabular-nums">{o.totalOrderItem ?? 0}</td>
                    <td className="px-4 py-3 font-medium">{formatVnd(o.totalAmount)}</td>
                    <td className="px-4 py-3">
                      <button
                        type="button"
                        disabled={terminal}
                        title={terminal ? "Đơn đã kết thúc" : undefined}
                        onClick={() => setEditing(o)}
                        className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-900 hover:bg-brand-100 disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        Sửa
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <StaffOrderEditDialog
        row={editing}
        waiters={waiters}
        tables={tables}
        onClose={() => setEditing(null)}
        onSaved={() => void load()}
      />
    </div>
  );
}
