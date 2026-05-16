"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { OrderModel, OrderStatus, OrderType, PaginatedResponse, TableModel } from "@/lib/api/types";
import { selectableOrderStatusesForStaff } from "@/lib/orders/order-status-options";
import { ORDER_TYPES, orderRequiresTable } from "@/lib/orders/order-type";
import { TABLE_STATUS_LABEL_VI } from "@/lib/tables/table-labels";

const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PREPARING: "Đang chuẩn bị",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const TYPE_LABEL: Record<OrderType, string> = {
  DINE_IN: "Tại chỗ",
  DELIVERY: "Giao hàng",
};

type Props = {
  row: OrderModel | null;
  /** @deprecated Giữ tương thích; luôn sửa đầy đủ. */
  statusOnly?: boolean;
  tables?: TableModel[];
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

export function StaffOrderEditDialog({ row, statusOnly = false, tables = [], onClose, onSaved }: Props) {
  const [customerName, setCustomerName] = useState("");
  const [customerPhone, setCustomerPhone] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [tableNumber, setTableNumber] = useState<number | "">("");
  const [orderStatus, setOrderStatus] = useState<OrderStatus>("PENDING");
  const [orderType, setOrderType] = useState<OrderType>("DINE_IN");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [tablesLive, setTablesLive] = useState<TableModel[]>([]);
  const [tablesLoading, setTablesLoading] = useState(false);

  const needsTable = orderRequiresTable(orderType);

  useEffect(() => {
    if (!row || statusOnly || !orderRequiresTable(orderType)) {
      setTablesLive([]);
      return;
    }
    let cancelled = false;
    setTablesLoading(true);
    void apiFetch<PaginatedResponse<TableModel>>(
      `/tables/filters?${buildPageParams(0, 200, { freshSnapshot: true })}`,
    )
      .then((res) => {
        if (!cancelled) {
          setTablesLive(res.data.content ?? []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setTablesLive(tables);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setTablesLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [row, statusOnly, tables, orderType]);

  const tablesSorted = useMemo(() => {
    if (statusOnly) {
      return [];
    }
    const source = tablesLive.length > 0 ? tablesLive : tables;
    return [...source].sort((a, b) => a.tableNumber - b.tableNumber);
  }, [statusOnly, tablesLive, tables]);

  const tableNumbersKnown = new Set(tablesSorted.map((t) => t.tableNumber));

  useEffect(() => {
    if (!needsTable) {
      return;
    }
    if (tableNumber === "" && tablesSorted.length > 0) {
      setTableNumber(tablesSorted[0].tableNumber);
    }
  }, [needsTable, tablesSorted, tableNumber]);

  function tableStatusLabel(status?: string): string {
    if (!status) {
      return "";
    }
    return TABLE_STATUS_LABEL_VI[status] ?? status;
  }

  useEffect(() => {
    if (!row) return;
    void Promise.resolve().then(() => {
      const options = selectableOrderStatusesForStaff(row);
      setCustomerName(row.customerName ?? "");
      setCustomerPhone((row.customerPhone ?? "").replace(/\D/g, "").slice(0, 11));
      setCustomerEmail(row.customerEmail ?? "");
      setTableNumber(
        row.orderType === "DELIVERY" ? "" : (row.tableNumber ?? ""),
      );
      setOrderStatus(options.includes(row.orderStatus) ? row.orderStatus : options[0] ?? row.orderStatus);
      setOrderType(row.orderType);
      setNotes(row.notes ?? "");
      setError(null);
    });
  }, [row]);

  if (!row) return null;

  const statusOptions = selectableOrderStatusesForStaff(row);
  const orphanTable =
    !statusOnly &&
    row.tableNumber != null &&
    !tableNumbersKnown.has(row.tableNumber);

  function buildUpdatePayload() {
    const phone = customerPhone.replace(/\D/g, "").slice(0, 11);
    if (statusOnly) {
      return {
        customerName: row!.customerName ?? "",
        customerPhone: (row!.customerPhone ?? "").replace(/\D/g, "").slice(0, 11),
        customerEmail: (row!.customerEmail ?? "").trim().toLowerCase(),
        tableNumber: row!.orderType === "DELIVERY" ? null : row!.tableNumber ?? null,
        orderStatus,
        orderType: row!.orderType,
        ...(row!.notes?.trim() ? { notes: row!.notes!.trim().slice(0, 300) } : {}),
      };
    }
    return {
      customerName: customerName.trim(),
      customerPhone: phone,
      customerEmail: customerEmail.trim().toLowerCase(),
      ...(needsTable ? { tableNumber } : { tableNumber: null }),
      orderStatus,
      orderType,
      ...(notes.trim() ? { notes: notes.trim().slice(0, 300) } : {}),
    };
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!row) return;
    setError(null);

    if (!statusOnly) {
      const phone = customerPhone.replace(/\D/g, "").slice(0, 11);
      if (phone.length < 10 || phone.length > 11) {
        setError("SĐT khách phải 10–11 chữ số");
        return;
      }
      if (needsTable && (tableNumber === "" || !tableNumbersKnown.has(tableNumber))) {
        setError("Chọn bàn trong danh sách hiện có.");
        return;
      }
    }

    setPending(true);
    try {
      await apiFetch<OrderModel[]>("/orders/admin", {
        method: "PUT",
        body: JSON.stringify({ ids: [row.id], updates: [buildUpdatePayload()] }),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Cập nhật thất bại");
    } finally {
      setPending(false);
    }
  }

  const canSave = statusOnly || !needsTable || tablesSorted.length > 0;

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
          {statusOnly ? "Cập nhật trạng thái" : "Sửa đơn"}{" "}
          <span className="font-mono text-[1.0625rem] font-semibold tabular-nums tracking-normal text-brand-900">
            {row.orderNumber}
          </span>
        </h2>
        <p className="mt-1 text-xs text-stone-500">
          {statusOnly
            ? "Bạn chỉ có thể đổi trạng thái đơn. Lưu sẽ gán bạn là nhân viên phục vụ."
            : "Lưu đơn sẽ gán bạn là nhân viên phục vụ cho đơn này."}
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          {statusOnly ? (
            <dl className="space-y-2 rounded-lg border border-stone-200 bg-stone-50/80 p-3 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-stone-500">Khách</dt>
                <dd className="text-right font-medium text-stone-800">{row.customerName ?? "—"}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-stone-500">Bàn</dt>
                <dd className="font-medium text-stone-800">{row.tableNumber}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-stone-500">Loại đơn</dt>
                <dd className="font-medium text-stone-800">{TYPE_LABEL[row.orderType]}</dd>
              </div>
              {row.notes ? (
                <div>
                  <dt className="text-stone-500">Ghi chú</dt>
                  <dd className="mt-1 text-stone-800">{row.notes}</dd>
                </div>
              ) : null}
            </dl>
          ) : (
            <>
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
              {needsTable ? (
              <label className="block text-xs font-medium text-stone-600">
                Bàn
                <select
                  className={fieldClass}
                  value={tableNumber}
                  onChange={(e) => setTableNumber(Number(e.target.value))}
                  required
                  disabled={tablesLoading && tablesSorted.length === 0}
                >
                  {tablesLoading && tablesSorted.length === 0 ? (
                    <option value={tableNumber}>Đang tải danh sách bàn…</option>
                  ) : null}
                  {orphanTable && row.tableNumber != null ? (
                    <option value={row.tableNumber}>
                      Bàn {row.tableNumber}
                      {tablesLoading ? " (đang cập nhật…)" : ""}
                    </option>
                  ) : null}
                  {tablesSorted.map((t) => (
                    <option key={t.id} value={t.tableNumber}>
                      Bàn {t.tableNumber}
                      {t.capacity ? ` · ${t.capacity} chỗ` : ""}
                      {t.tableStatus ? ` · ${tableStatusLabel(t.tableStatus)}` : ""}
                    </option>
                  ))}
                </select>
              </label>
              ) : (
                <p className="text-xs text-stone-500">Đơn giao hàng không gắn bàn.</p>
              )}
              <label className="block text-xs font-medium text-stone-600">
                Ghi chú
                <textarea className={fieldClass} value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} maxLength={300} />
              </label>
            </>
          )}

          <label className="block text-xs font-medium text-stone-600">
            Trạng thái đơn
            <select className={fieldClass} value={orderStatus} onChange={(e) => setOrderStatus(e.target.value as OrderStatus)}>
              {statusOptions.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABEL[s]}
                </option>
              ))}
            </select>
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