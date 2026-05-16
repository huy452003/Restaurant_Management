"use client";

import type { TableModel } from "@/lib/api/types";
import { TABLE_STATUS_LABEL_VI } from "@/lib/tables/table-labels";

const selectClass =
  "w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25 disabled:cursor-not-allowed disabled:bg-stone-50";

type Props = {
  id?: string;
  label?: string;
  value: number | "";
  onChange: (tableNumber: number) => void;
  tables: TableModel[];
  loading?: boolean;
  error?: string | null;
  showStatus?: boolean;
  disabled?: boolean;
  required?: boolean;
  emptyHint?: string;
};

export function TableNumberSelect({
  id,
  label = "Số bàn",
  value,
  onChange,
  tables,
  loading = false,
  error = null,
  showStatus = true,
  disabled = false,
  required = true,
  emptyHint = "Chưa có bàn phù hợp trong hệ thống.",
}: Props) {
  return (
    <div>
      <label htmlFor={id} className="block text-xs font-medium text-stone-600 sm:text-sm sm:text-stone-700">
        {label}
      </label>
      {error ? <p className="mt-1 text-xs text-red-700">{error}</p> : null}
      {loading ? (
        <select id={id} disabled className={`mt-1 ${selectClass}`}>
          <option>Đang tải danh sách bàn…</option>
        </select>
      ) : tables.length === 0 ? (
        <p className="mt-1 text-sm text-muted">{emptyHint}</p>
      ) : (
        <select
          id={id}
          required={required}
          disabled={disabled}
          value={value === "" ? "" : String(value)}
          onChange={(e) => onChange(Number(e.target.value))}
          className={`mt-1 ${selectClass}`}
        >
          {value === "" ? <option value="">Chọn bàn</option> : null}
          {tables.map((t) => (
            <option key={t.id} value={t.tableNumber}>
              Bàn {t.tableNumber}
              {t.capacity ? ` · ${t.capacity} chỗ` : ""}
              {showStatus && t.tableStatus
                ? ` · ${TABLE_STATUS_LABEL_VI[t.tableStatus] ?? t.tableStatus}`
                : ""}
            </option>
          ))}
        </select>
      )}
    </div>
  );
}

