"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffPaymentEditDialog } from "@/components/staff/StaffPaymentEditDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import type { PaymentModel } from "@/lib/api/types";
import { initVnpayCheckout } from "@/lib/payments/init-vnpay";
import { LIST_EXTRA_SORT_NEWEST, PAYMENT_LIST_FILTERS } from "@/lib/list/presets";
import { formatVnd } from "@/lib/money";
import { StatusBadge } from "@/components/ui/StatusBadge";

const METHOD_LABEL: Record<string, string> = {
  CASH: "Tiền mặt",
  VNPAY: "VNPAY",
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Chờ thanh toán",
  COMPLETED: "Đã thanh toán",
  FAILED: "Thất bại",
  CANCELLED: "Đã hủy",
};

function isPaymentPending(p: PaymentModel): boolean {
  return (p.paymentStatus?.toUpperCase() ?? "") === "PENDING";
}

export default function StaffPaymentsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [editing, setEditing] = useState<PaymentModel | null>(null);
  const [vnpayError, setVnpayError] = useState<string | null>(null);
  const [vnpayPendingId, setVnpayPendingId] = useState<number | null>(null);

  const {
    rows,
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
  } = usePaginatedList<PaymentModel>({
    basePath: "/payments/filters",
    pageSize: 15,
    extraParams: LIST_EXTRA_SORT_NEWEST,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/payments");
      return;
    }
    if (!hasRole("CASHIER", "MANAGER", "ADMIN")) {
      router.replace("/staff");
      return;
    }
    loadInitial();
  }, [user, authLoading, hasRole, router, loadInitial]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Thanh toán
      </h1>
      <p className="mt-1 text-sm text-muted">Tạo thanh toán mới từ trang Đơn hàng.</p>
      <FilterBar
        fields={PAYMENT_LIST_FILTERS}
        values={draftFilters}
        onChange={setFilter}
        onApply={applyFilters}
        onReset={resetFilters}
        loading={loading}
      />

      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {vnpayError ? <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{vnpayError}</p> : null}
      {loading && rows.length === 0 ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <>
        <div className="mt-6 overflow-x-auto rounded-xl border border-stone-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-stone-100 text-stone-600">
              <tr>
                <th className="px-3 py-3 font-medium">#</th>
                <th className="px-3 py-3 font-medium">Đơn</th>
                <th className="px-3 py-3 font-medium">Thu ngân</th>
                <th className="px-3 py-3 font-medium">Phương thức</th>
                <th className="px-3 py-3 font-medium">Số tiền</th>
                <th className="px-3 py-3 font-medium">Trạng thái</th>
                <th className="px-3 py-3 font-medium">Mã GD</th>
                <th className="px-3 py-3 font-medium w-28">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((p) => {
                const pending = isPaymentPending(p);
                const isVnpay = (p.paymentMethod?.toUpperCase() ?? "") === "VNPAY";
                async function openVnpayAgain() {
                  const on = p.orderNumber?.trim();
                  if (!on) return;
                  setVnpayError(null);
                  setVnpayPendingId(p.id);
                  const result = await initVnpayCheckout(on);
                  setVnpayPendingId(null);
                  if (!result.ok) {
                    setVnpayError(result.message);
                    return;
                  }
                  reload();
                }
                return (
                <tr key={p.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                  <td className="px-3 py-2.5 font-mono text-xs tabular-nums text-stone-600">{p.id}</td>
                  <td className="px-3 py-2.5 font-mono text-xs">{p.orderNumber ?? "—"}</td>
                  <td className="max-w-[140px] px-3 py-2.5 text-muted">
                    <span className="line-clamp-2 break-words">{p.cashierFullname ?? "—"}</span>
                  </td>
                  <td className="px-3 py-2.5">{METHOD_LABEL[p.paymentMethod] ?? p.paymentMethod}</td>
                  <td className="px-3 py-2.5 font-medium tabular-nums">{formatVnd(p.amount)}</td>
                  <td className="px-3 py-2.5">
                    <StatusBadge domain="payment" status={p.paymentStatus} />
                  </td>
                  <td className="max-w-[120px] px-3 py-2.5">
                    <span className="line-clamp-2 break-all font-mono text-xs text-stone-600">
                      {p.transactionId ?? "—"}
                    </span>
                  </td>
                  <td className="px-3 py-2.5">
                    <button
                      type="button"
                      disabled={vnpayPendingId === p.id}
                      onClick={() => {
                        if (pending && isVnpay) {
                          void openVnpayAgain();
                          return;
                        }
                        setEditing(p);
                      }}
                      className={
                        pending
                          ? "rounded-lg border border-violet-200 bg-violet-50 px-3 py-1.5 text-xs font-semibold text-violet-900 hover:bg-violet-100"
                          : "rounded-lg border border-stone-200 px-3 py-1.5 text-xs font-medium text-stone-700 hover:bg-stone-50"
                      }
                    >
                      {vnpayPendingId === p.id
                        ? "Đang mở…"
                        : pending && isVnpay
                          ? "Mở VNPAY"
                          : pending
                            ? "Xử lý"
                            : "Chi tiết"}
                    </button>
                  </td>
                </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        {rows.length === 0 ? <p className="mt-4 text-sm text-muted">Không có kết quả phù hợp.</p> : null}
        <PaginationBar
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          loading={loading}
          onPageChange={goToPage}
          unitLabel="giao dịch"
        />
        </>
      )}

      <StaffPaymentEditDialog
        open={editing != null}
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={reload}
      />
    </div>
  );
}
