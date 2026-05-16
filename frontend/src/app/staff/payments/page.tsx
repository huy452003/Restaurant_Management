"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffPaymentEditDialog } from "@/components/staff/StaffPaymentEditDialog";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { PaginatedResponse, PaymentModel } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";

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

function isPaymentTerminal(p: PaymentModel): boolean {
  const s = p.paymentStatus?.toUpperCase() ?? "";
  return s === "COMPLETED" || s === "CANCELLED";
}

export default function StaffPaymentsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [rows, setRows] = useState<PaymentModel[]>([]);
  const [editing, setEditing] = useState<PaymentModel | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<PaymentModel>>(`/payments/filters?${buildPageParams(0, 80)}`);
      setRows(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được thanh toán");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

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
    void Promise.resolve().then(() => load());
  }, [user, authLoading, hasRole, router, load]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Thanh toán
      </h1>
      <p className="mt-1 text-sm text-muted">Tạo thanh toán mới từ trang Đơn hàng.</p>
      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
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
                const terminal = isPaymentTerminal(p);
                return (
                <tr key={p.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                  <td className="px-3 py-2.5 font-mono text-xs tabular-nums text-stone-600">{p.id}</td>
                  <td className="px-3 py-2.5 font-mono text-xs">{p.orderNumber ?? "—"}</td>
                  <td className="max-w-[140px] px-3 py-2.5 text-muted">
                    <span className="line-clamp-2 break-words">{p.cashierFullname ?? "—"}</span>
                  </td>
                  <td className="px-3 py-2.5">{METHOD_LABEL[p.paymentMethod] ?? p.paymentMethod}</td>
                  <td className="px-3 py-2.5 font-medium tabular-nums">{formatVnd(p.amount)}</td>
                  <td className="px-3 py-2.5">{STATUS_LABEL[p.paymentStatus] ?? p.paymentStatus}</td>
                  <td className="max-w-[120px] px-3 py-2.5">
                    <span className="line-clamp-2 break-all font-mono text-xs text-stone-600">
                      {p.transactionId ?? "—"}
                    </span>
                  </td>
                  <td className="px-3 py-2.5">
                    <button
                      type="button"
                      disabled={terminal}
                      title={terminal ? "Thanh toán đã kết thúc" : undefined}
                      onClick={() => setEditing(p)}
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

      <StaffPaymentEditDialog
        open={editing != null}
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={() => void load()}
      />
    </div>
  );
}
