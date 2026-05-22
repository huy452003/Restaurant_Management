"use client";

import Link from "next/link";
import { TableNumberSelect } from "@/components/TableNumberSelect";
import type { TableModel } from "@/lib/api/types";
import type { CartLine } from "@/lib/cart/types";

type Props = {
  lines: CartLine[];
  tableNumber: number | "";
  onTableNumberChange: (n: number) => void;
  tables: TableModel[];
  tablesLoading: boolean;
  tablesError: string | null;
  notes: string;
  onNotesChange: (v: string) => void;
  onIncrement: (id: number) => void;
  onDecrement: (id: number) => void;
  onClearCart: () => void;
  onSubmit: () => void;
  submitting: boolean;
  message: string | null;
  lastOrderNumber: string | null;
};

export function StaffServeOrderPanel({
  lines,
  tableNumber,
  onTableNumberChange,
  tables,
  tablesLoading,
  tablesError,
  notes,
  onNotesChange,
  onIncrement,
  onDecrement,
  onClearCart,
  onSubmit,
  submitting,
  message,
  lastOrderNumber,
}: Props) {
  const itemCount = lines.reduce((sum, l) => sum + l.quantity, 0);
  const canSubmit = lines.length > 0 && tableNumber !== "" && tables.length > 0 && !tablesLoading;

  return (
    <div className="overflow-hidden rounded-2xl border border-stone-200/90 bg-surface shadow-md lg:sticky lg:top-20">
      <div className="border-b border-brand-100 bg-gradient-to-br from-brand-50 to-surface px-4 py-4 lg:px-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2
              className="font-serif text-xl font-semibold text-brand-900"
              style={{ fontFamily: "var(--font-cormorant), serif" }}
            >
              Đơn tại chỗ
            </h2>
            <p className="mt-0.5 text-xs leading-relaxed text-muted">
              Chọn bàn, thêm món rồi gửi bếp.
            </p>
          </div>
          {itemCount > 0 ? (
            <span className="shrink-0 rounded-full bg-brand-800 px-2.5 py-0.5 text-xs font-semibold text-white tabular-nums">
              {itemCount} món
            </span>
          ) : null}
        </div>
      </div>

      <div className="space-y-4 px-4 py-4 lg:px-5">
        <TableNumberSelect
          id="staff-serve-table"
          label="Bàn phục vụ"
          value={tableNumber}
          onChange={onTableNumberChange}
          tables={tables}
          loading={tablesLoading}
          error={tablesError}
          emptyHint="Không có bàn trống."
        />

        <div>
          <label htmlFor="staff-serve-notes" className="block text-xs font-semibold uppercase tracking-wide text-stone-500">
            Ghi chú đơn
          </label>
          <textarea
            id="staff-serve-notes"
            value={notes}
            onChange={(e) => onNotesChange(e.target.value)}
            rows={2}
            maxLength={300}
            placeholder="Tên khách, yêu cầu chung…"
            className="mt-1.5 w-full resize-none rounded-xl border border-stone-200 bg-stone-50/50 px-3 py-2.5 text-sm text-stone-900 outline-none transition focus:border-brand-300 focus:bg-white focus:ring-2 focus:ring-brand-600/20"
          />
        </div>

        <section className="rounded-xl border border-stone-100 bg-stone-50/40 p-3">
          <div className="flex items-center justify-between gap-2">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-stone-500">Món đã chọn</h3>
            {lines.length > 0 ? (
              <button
                type="button"
                onClick={onClearCart}
                className="text-xs font-medium text-stone-500 transition hover:text-red-600"
              >
                Xóa hết
              </button>
            ) : null}
          </div>

          {lines.length === 0 ? (
            <p className="mt-3 rounded-lg border border-dashed border-stone-200 bg-white/80 px-3 py-6 text-center text-sm text-muted">
              Chưa có món — bấm <span className="font-medium text-brand-800">Thêm</span> ở thực đơn bên trái.
            </p>
          ) : (
            <ul className="mt-2 max-h-56 space-y-1.5 overflow-y-auto pr-0.5">
              {lines.map((line) => (
                <li
                  key={line.item.id}
                  className="flex items-center justify-between gap-2 rounded-lg border border-stone-100 bg-white px-2.5 py-2 shadow-sm"
                >
                  <p className="min-w-0 flex-1 truncate text-sm font-medium text-stone-900">{line.item.name}</p>
                  <div className="flex shrink-0 items-center gap-0.5 rounded-lg bg-stone-50 p-0.5 ring-1 ring-stone-100">
                    <button
                      type="button"
                      onClick={() => onDecrement(line.item.id)}
                      className="flex h-7 w-7 items-center justify-center rounded-md text-stone-600 transition hover:bg-white hover:text-brand-800"
                      aria-label="Giảm"
                    >
                      −
                    </button>
                    <span className="min-w-[1.75rem] text-center text-xs font-bold tabular-nums text-brand-900">
                      {line.quantity}
                    </span>
                    <button
                      type="button"
                      onClick={() => onIncrement(line.item.id)}
                      className="flex h-7 w-7 items-center justify-center rounded-md text-stone-600 transition hover:bg-white hover:text-brand-800"
                      aria-label="Tăng"
                    >
                      +
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {message ? (
          <p
            className={`rounded-xl px-3 py-2.5 text-sm leading-snug ${
              lastOrderNumber
                ? "bg-emerald-50 text-emerald-900 ring-1 ring-emerald-100"
                : "bg-red-50 text-red-800 ring-1 ring-red-100"
            }`}
          >
            {message}{" "}
            {lastOrderNumber ? (
              <Link href="/staff/orders" className="font-semibold text-emerald-800 underline underline-offset-2">
                Xem đơn hàng
              </Link>
            ) : null}
          </p>
        ) : null}
      </div>

      <div className="border-t border-stone-100 bg-stone-50/50 px-4 py-4 lg:px-5">
        <button
          type="button"
          disabled={!canSubmit || submitting}
          onClick={onSubmit}
          className="w-full rounded-xl bg-brand-800 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-brand-900 hover:shadow disabled:cursor-not-allowed disabled:bg-stone-300 disabled:text-stone-500 disabled:shadow-none"
        >
          {submitting ? "Đang gửi đơn…" : "Xác nhận & gửi bếp"}
        </button>
      </div>
    </div>
  );
}
