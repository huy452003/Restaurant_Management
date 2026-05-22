"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffOrderEditDialog } from "@/components/staff/StaffOrderEditDialog";
import { StaffPaymentCreateDialog } from "@/components/staff/StaffPaymentCreateDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import { apiFetch, buildPageParams } from "@/lib/api/client";
import type { OrderModel, OrderStatus, PaginatedResponse, TableModel } from "@/lib/api/types";
import { LIST_EXTRA_SORT_NEWEST, ORDER_ADMIN_LIST_FILTERS } from "@/lib/list/presets";
import { canCreatePaymentForOrder } from "@/lib/orders/order-payment";
import { formatVnd } from "@/lib/money";
import { StatusBadge } from "@/components/ui/StatusBadge";

function isTerminal(s: OrderStatus): boolean {
  return s === "COMPLETED" || s === "CANCELLED";
}

export default function StaffOrdersPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const canManagePayment = hasRole("CASHIER", "MANAGER", "ADMIN");
  const canEditOrder = hasRole("ADMIN", "MANAGER", "CASHIER");
  const router = useRouter();
  const [tables, setTables] = useState<TableModel[]>([]);
  const [editing, setEditing] = useState<OrderModel | null>(null);
  const [payingOrder, setPayingOrder] = useState<OrderModel | null>(null);

  const {
    rows: orders,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    draftFilters,
    setFilter,
    applyFilters,
    resetFilters,
    reload,
    goToPage,
    loadInitial,
  } = usePaginatedList<OrderModel>({
    basePath: "/orders/filters/admin",
    pageSize: 15,
    extraParams: LIST_EXTRA_SORT_NEWEST,
  });

  const loadTables = useCallback(async () => {
    try {
      const res = await apiFetch<PaginatedResponse<TableModel>>(
        `/tables/filters?${buildPageParams(0, 200, { tableStatus: "AVAILABLE", freshSnapshot: true })}`,
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
    if (!canEditOrder && !canManagePayment) {
      router.replace("/staff");
      return;
    }
    loadInitial();
    if (canEditOrder) {
      void loadTables();
    }
  }, [user, authLoading, canEditOrder, canManagePayment, router, loadInitial, loadTables]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Đơn hàng
      </h1>

      <FilterBar
        fields={ORDER_ADMIN_LIST_FILTERS}
        values={draftFilters}
        onChange={setFilter}
        onApply={applyFilters}
        onReset={resetFilters}
        loading={loading}
      />

      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading && orders.length === 0 ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <>
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
                <th className="px-4 py-3 font-medium">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => {
                const terminal = isTerminal(o.orderStatus);
                const showPay = canManagePayment && canCreatePaymentForOrder(o);
                const showEdit = canEditOrder && !terminal;
                return (
                  <tr key={o.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                    <td className="px-4 py-3 font-mono text-xs">{o.orderNumber}</td>
                    <td className="px-4 py-3">{o.tableNumber ?? "—"}</td>
                    <td className="px-4 py-3 text-muted">{o.customerName ?? "—"}</td>
                    <td className="px-4 py-3">
                      <StatusBadge domain="order" status={o.orderStatus} />
                    </td>
                    <td className="px-4 py-3 tabular-nums">{o.totalOrderItem ?? 0}</td>
                    <td className="px-4 py-3 font-medium">{formatVnd(o.totalAmount)}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        {showEdit ? (
                          <button
                            type="button"
                            onClick={() => {
                              void loadTables();
                              setEditing(o);
                            }}
                            className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-900 hover:bg-brand-100"
                          >
                            Sửa
                          </button>
                        ) : null}
                        {showPay ? (
                          <button
                            type="button"
                            onClick={() => {
                              setEditing(null);
                              setPayingOrder(o);
                            }}
                            className="rounded-lg border border-violet-200 bg-violet-50 px-3 py-1.5 text-xs font-semibold text-violet-900 hover:bg-violet-100"
                          >
                            Thanh toán
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {orders.length === 0 ? <p className="mt-4 text-sm text-muted">Không có kết quả phù hợp.</p> : null}
        <PaginationBar
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          loading={loading}
          onPageChange={goToPage}
          unitLabel="đơn"
        />
        </>
      )}

      {canEditOrder ? (
        <StaffOrderEditDialog
          row={editing}
          tables={tables}
          onClose={() => setEditing(null)}
          onSaved={reload}
        />
      ) : null}

      {canManagePayment ? (
        <StaffPaymentCreateDialog
          open={payingOrder != null}
          initialOrderNumber={payingOrder?.orderNumber}
          onClose={() => setPayingOrder(null)}
          onSaved={reload}
        />
      ) : null}
    </div>
  );
}