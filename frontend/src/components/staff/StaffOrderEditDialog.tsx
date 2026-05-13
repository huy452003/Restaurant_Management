"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { OrderModel, OrderStatus, OrderType, TableModel, UserModel } from "@/lib/api/types";

const ORDER_STATUSES: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "PREPARING",
  "READY",
  "SERVED",
  "COMPLETED",
  "CANCELLED",
];

const ORDER_TYPES: OrderType[] = ["DINE_IN", "TAKE_AWAY", "DELIVERY"];

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang chuẩn bị",
  READY: "Sẵn sàng",
  SERVED: "Đã phục vụ",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const TYPE_LABEL: Record<OrderType, string> = {
  DINE_IN: "Tại chỗ",
  TAKE_AWAY: "Mang về",
  DELIVERY: "Giao hàng",
};

type Props = {
  row: OrderModel | null;
  waiters: UserModel[];
  tables: TableModel[];
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

export function StaffOrderEditDialog({ row, waiters, tables, onClose, onSaved }: Props) {
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [tableNumber, setTableNumber] = useState(1);
  const [waiterId, setWaiterId] = useState<number | "">("");
  const [orderStatus, setOrderStatus] = useState<OrderStatus>("PENDING");
  const [orderType, setOrderType] = useState<OrderType>("DINE_IN");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const tablesSorted = useMemo(
    () => [...tables].sort((a, b) => a.tableNumber - b.tableNumber),
    [tables],
  );

  const tableNumbersKnown = useMemo(() => new Set(tablesSorted.map((t) => t.tableNumber)), [tablesSorted]);

  useEffect(() => {
    if (!row) return;
    void Promise.resolve().then(() => {
      setCustomerName(row.customerName ?? "");
      setCustomerPhone((row.customerPhone ?? "").replace(/\D/g, "").slice(0, 11));
      setCustomerEmail(row.customerEmail ?? "");
      setTableNumber(row.tableNumber ?? 1);
      setWaiterId(row.waiterId ?? (waiters[0]?.id ?? ""));
      setOrderStatus(row.orderStatus);
      setOrderType(row.orderType);
      setNotes(row.notes ?? "");
      setError(null);
    });
  }, [row, waiters]);

  if (!row) return null;

  const orphanTable = !tableNumbersKnown.has(row.tableNumber);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!row) return;
    setError(null);
    const phone = customerPhone.replace(/\D/g, "").slice(0, 11);
    if (phone.length < 10 || phone.length > 11) {
      setError("SĐT khách phải 10–11 chữ số");
      return;
    }
    if (waiterId === "" || waiterId < 1) {
      setError("Chọn nhân viên phục vụ (WAITER)");
      return;
    }
    if (!tableNumbersKnown.has(tableNumber)) {
      setError("Chọn bàn trong danh sách hiện có.");
      return;
    }
    setPending(true);
    try {
      const update = {
        customerName: customerName.trim(),
        customerPhone: phone,
        customerEmail: customerEmail.trim().toLowerCase(),
        tableNumber,
        waiterId: Number(waiterId),
        orderStatus,
        orderType,
        ...(notes.trim() ? { notes: notes.trim().slice(0, 300) } : {}),
      };
      await apiFetch<OrderModel[]>("/orders/admin", {
        method: "PUT",
        body: JSON.stringify({ ids: [row.id], updates: [update] }),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Cập nhật thất bại");
    } finally {
      setPending(false);
    }
  }

  const canSave = waiters.length > 0 && tablesSorted.length > 0;

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
        aria-labelledby="edit-order-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="edit-order-title" className="font-serif text-xl font-semibold text-brand-900">
          Sửa đơn{" "}
          <span className="font-mono text-[1.0625rem] font-semibold tabular-nums tracking-normal text-brand-900">
            {row.orderNumber}
          </span>
        </h2>

        <form onSubmit={handleSubmit} className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          <label className="block text-xs font-medium text-stone-600">
            Tên khách
            <input className={fieldClass} value={customerName} onChange={(e) => setCustomerName(e.target.value)} required maxLength={50} />
          </label>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-medium text-stone-600">
              SĐT khách (10–11 số)
              <input
                className={fieldClass}
                value={customerPhone}
                onChange={(e) => setCustomerPhone(e.target.value.replace(/\D/g, "").slice(0, 11))}
                required
                pattern="[0-9]{10,11}"
              />
            </label>
            <label className="block text-xs font-medium text-stone-600">
              Email khách
              <input type="email" className={fieldClass} value={customerEmail} onChange={(e) => setCustomerEmail(e.target.value)} required />
            </label>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-medium text-stone-600">
              Bàn
              <select
                className={fieldClass}
                value={tableNumber}
                onChange={(e) => setTableNumber(Number(e.target.value))}
                required
              >
                {orphanTable ? (
                  <option value={row.tableNumber}>Bàn {row.tableNumber}</option>
                ) : null}
                {tablesSorted.map((t) => (
                  <option key={t.id} value={t.tableNumber}>
                    Bàn {t.tableNumber}
                    {t.capacity ? ` · ${t.capacity} chỗ` : ""}
                    {t.tableStatus ? ` · ${t.tableStatus}` : ""}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-xs font-medium text-stone-600">
              Phục vụ (WAITER)
              <select
                className={fieldClass}
                value={waiterId === "" ? "" : String(waiterId)}
                onChange={(e) => setWaiterId(e.target.value ? Number(e.target.value) : "")}
                required
              >
                <option value="">— Chọn —</option>
                {row.waiterId != null && !waiters.some((w) => w.id === row.waiterId) ? (
                  <option value={row.waiterId}>Phục vụ hiện tại (#{row.waiterId})</option>
                ) : null}
                {waiters.map((w) => (
                  <option key={w.id} value={w.id ?? ""}>
                    {w.fullname ?? w.username} (#{w.id})
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-medium text-stone-600">
              Trạng thái đơn
              <select className={fieldClass} value={orderStatus} onChange={(e) => setOrderStatus(e.target.value as OrderStatus)}>
                {ORDER_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_LABEL[s]}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-xs font-medium text-stone-600">
              Loại đơn
              <select className={fieldClass} value={orderType} onChange={(e) => setOrderType(e.target.value as OrderType)}>
                {ORDER_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {TYPE_LABEL[t]}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <label className="block text-xs font-medium text-stone-600">
            Ghi chú
            <textarea className={fieldClass} value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} maxLength={300} />
          </label>

          <div className="flex flex-wrap justify-end gap-3 pt-4">
            <button type="button" onClick={onClose} className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium hover:bg-stone-50">
              Hủy
            </button>
            <button
              type="submit"
              disabled={pending || !canSave}
              className="rounded-lg bg-brand-800 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50"
            >
              {pending ? "Đang lưu…" : "Lưu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
