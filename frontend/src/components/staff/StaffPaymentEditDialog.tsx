"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { PaymentModel } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";

type Props = {
  open: boolean;
  row: PaymentModel | null;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

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

type PendingAction = "COMPLETED" | "CANCELLED";

export function StaffPaymentEditDialog({ open, row, onClose, onSaved }: Props) {
  const [action, setAction] = useState<PendingAction>("COMPLETED");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!open || !row) return;
    setError(null);
    setAction("COMPLETED");
  }, [open, row]);

  if (!open || !row) return null;

  const editingRow = row;
  const isPending = editingRow.paymentStatus === "PENDING";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!isPending) return;
    setPending(true);
    try {
      const path =
        action === "COMPLETED"
          ? `/payments/complete/${editingRow.id}`
          : `/payments/cancel/${editingRow.id}`;
      await apiFetch<PaymentModel>(path, { method: "PATCH" });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Cập nhật thất bại");
    } finally {
      setPending(false);
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
        aria-labelledby="edit-payment-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="edit-payment-title" className="font-serif text-xl font-semibold text-brand-900">
          Thanh toán{" "}
          <span className="font-mono text-[1.0625rem] font-semibold tabular-nums tracking-normal text-brand-900">
            #{editingRow.id}
          </span>
        </h2>

        <dl className="mt-4 space-y-2 text-sm">
          <div className="flex justify-between gap-2 border-b border-stone-100 py-1">
            <dt className="text-stone-500">Đơn</dt>
            <dd className="font-mono text-xs">{editingRow.orderNumber ?? "—"}</dd>
          </div>
          <div className="flex justify-between gap-2 border-b border-stone-100 py-1">
            <dt className="text-stone-500">Thu ngân</dt>
            <dd>{editingRow.cashierFullname ?? "—"}</dd>
          </div>
          <div className="flex justify-between gap-2 border-b border-stone-100 py-1">
            <dt className="text-stone-500">Phương thức</dt>
            <dd>{METHOD_LABEL[editingRow.paymentMethod] ?? editingRow.paymentMethod}</dd>
          </div>
          <div className="flex justify-between gap-2 border-b border-stone-100 py-1">
            <dt className="text-stone-500">Số tiền</dt>
            <dd className="font-medium">{formatVnd(editingRow.amount)}</dd>
          </div>
          <div className="flex justify-between gap-2 border-b border-stone-100 py-1">
            <dt className="text-stone-500">Trạng thái</dt>
            <dd>{STATUS_LABEL[editingRow.paymentStatus] ?? editingRow.paymentStatus}</dd>
          </div>
          {editingRow.transactionId ? (
            <div className="flex justify-between gap-2 border-b border-stone-100 py-1">
              <dt className="text-stone-500">Mã giao dịch</dt>
              <dd className="max-w-[220px] break-all font-mono text-xs">{editingRow.transactionId}</dd>
            </div>
          ) : null}
          {editingRow.paidAt ? (
            <div className="flex justify-between gap-2 py-1">
              <dt className="text-stone-500">Thời điểm thanh toán</dt>
              <dd className="text-xs">{editingRow.paidAt}</dd>
            </div>
          ) : null}
        </dl>

        {isPending ? (
          <form onSubmit={handleSubmit} className="mt-6 space-y-3">
            {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}
            <label className="block text-xs font-medium text-stone-600">
              Chuyển trạng thái
              <select
                className={fieldClass}
                value={action}
                onChange={(e) => setAction(e.target.value as PendingAction)}
                required
              >
                <option value="COMPLETED">Hoàn thành</option>
                <option value="CANCELLED">Hủy</option>
              </select>
            </label>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium text-stone-700 hover:bg-stone-50"
              >
                Đóng
              </button>
              <button
                type="submit"
                disabled={pending}
                className="rounded-lg border border-brand-200 bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
              >
                {pending ? "Đang xử lý…" : "Áp dụng"}
              </button>
            </div>
          </form>
        ) : (
          <div className="mt-6 flex justify-end">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium text-stone-700 hover:bg-stone-50"
            >
              Đóng
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
