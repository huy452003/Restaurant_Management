"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { OrderItemModel, OrderModel, PaginatedResponse } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";
import {
  ORDER_ITEM_STATUS_LABEL,
  ORDER_STATUS_LABEL,
  ORDER_TYPE_LABEL,
} from "@/lib/orders/order-labels";

type Props = {
  open: boolean;
  order: OrderModel | null;
  onClose: () => void;
  /** Mở form chỉnh sửa (đơn PENDING, customer). */
  onEdit?: () => void;
};

export function OrderDetailDialog({ open, order, onClose, onEdit }: Props) {
  const [items, setItems] = useState<OrderItemModel[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !order) {
      setItems([]);
      setError(null);
      return;
    }
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const qs = buildPageParams(0, 100, { orderNumber: order.orderNumber });
        const res = await apiFetch<PaginatedResponse<OrderItemModel>>(`/order-items/filters?${qs}`);
        if (!cancelled) setItems(res.data.content ?? []);
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof ApiError ? e.message : "Không tải được món trong đơn");
          setItems([]);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [open, order]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open || !order) return null;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-stone-200 bg-surface p-6 shadow-xl"
        role="dialog"
        aria-labelledby="order-detail-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="order-detail-title"
              className="font-serif text-xl font-semibold text-brand-900"
              style={{ fontFamily: "var(--font-cormorant), serif" }}
            >
              Chi tiết đơn
            </h2>
            <p className="mt-1 font-mono text-sm font-semibold text-brand-800">{order.orderNumber}</p>
          </div>
          <div className="flex shrink-0 flex-wrap gap-2">
            {onEdit ? (
              <button
                type="button"
                onClick={onEdit}
                className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-sm font-semibold text-brand-900 hover:bg-brand-100"
              >
                Chỉnh sửa
              </button>
            ) : null}
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-stone-200 px-3 py-1.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
            >
              Đóng
            </button>
          </div>
        </div>

        <dl className="mt-5 grid gap-2 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Trạng thái</dt>
            <dd className="font-medium text-stone-900">{ORDER_STATUS_LABEL[order.orderStatus]}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Loại đơn</dt>
            <dd className="font-medium text-stone-900">{ORDER_TYPE_LABEL[order.orderType]}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Bàn</dt>
            <dd className="font-medium text-stone-900">Bàn {order.tableNumber}</dd>
          </div>
          {order.totalOrderItem != null ? (
            <div>
              <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Số món</dt>
              <dd className="font-medium text-stone-900">{order.totalOrderItem}</dd>
            </div>
          ) : null}
          {order.createdAt ? (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Tạo lúc</dt>
              <dd className="text-stone-800">{order.createdAt}</dd>
            </div>
          ) : null}
          {order.notes ? (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Ghi chú</dt>
              <dd className="text-stone-800">{order.notes}</dd>
            </div>
          ) : null}
        </dl>

        {order.subTotal != null || order.tax != null || order.totalAmount != null ? (
          <div className="mt-4 rounded-xl bg-stone-50 px-4 py-3 text-sm">
            {order.subTotal != null ? (
              <p className="flex justify-between gap-4">
                <span className="text-muted">Tạm tính</span>
                <span className="font-medium tabular-nums">{formatVnd(order.subTotal)}</span>
              </p>
            ) : null}
            {order.tax != null ? (
              <p className="mt-1 flex justify-between gap-4">
                <span className="text-muted">Thuế</span>
                <span className="font-medium tabular-nums">{formatVnd(order.tax)}</span>
              </p>
            ) : null}
            {order.totalAmount != null ? (
              <p className="mt-2 flex justify-between gap-4 border-t border-stone-200 pt-2">
                <span className="font-semibold text-stone-800">Tổng cộng</span>
                <span className="text-lg font-semibold text-brand-800 tabular-nums">
                  {formatVnd(order.totalAmount)}
                </span>
              </p>
            ) : null}
          </div>
        ) : null}

        <h3 className="mt-6 text-sm font-semibold text-stone-800">Món đã đặt</h3>
        {error ? (
          <p className="mt-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>
        ) : null}
        {loading ? (
          <p className="mt-3 text-sm text-muted">Đang tải món…</p>
        ) : items.length === 0 ? (
          <p className="mt-3 text-sm text-muted">Chưa có món trong đơn.</p>
        ) : (
          <div className="mt-3 overflow-x-auto rounded-xl border border-stone-200">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-stone-50 text-xs font-medium uppercase tracking-wide text-stone-500">
                <tr>
                  <th className="px-3 py-2.5">Món</th>
                  <th className="px-3 py-2.5">SL</th>
                  <th className="px-3 py-2.5">Đơn giá</th>
                  <th className="px-3 py-2.5">Thành tiền</th>
                  <th className="px-3 py-2.5">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {items.map((line) => (
                  <tr key={line.id}>
                    <td className="px-3 py-2.5">
                      <span className="font-medium text-stone-900">{line.menuItemName}</span>
                      {line.specialInstructions ? (
                        <p className="mt-0.5 text-xs text-muted">{line.specialInstructions}</p>
                      ) : null}
                    </td>
                    <td className="px-3 py-2.5 tabular-nums">{line.quantity}</td>
                    <td className="px-3 py-2.5 tabular-nums whitespace-nowrap">
                      {formatVnd(line.unitPrice)}
                    </td>
                    <td className="px-3 py-2.5 tabular-nums whitespace-nowrap font-medium">
                      {formatVnd(line.subTotal)}
                    </td>
                    <td className="px-3 py-2.5 text-xs">
                      {ORDER_ITEM_STATUS_LABEL[line.orderItemStatus] ?? line.orderItemStatus}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
