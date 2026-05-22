"use client";

import { useMemo, useState } from "react";

export type FilterField =
  | { key: string; label: string; type: "text"; placeholder?: string }
  | { key: string; label: string; type: "number"; placeholder?: string }
  | {
      key: string;
      label: string;
      type: "select";
      emptyOption?: string;
      options: readonly { value: string; label: string }[];
    };

export type FilterValues = Record<string, string>;

type Props = {
  fields: FilterField[];
  values: FilterValues;
  onChange: (key: string, value: string) => void;
  onApply: () => void;
  onReset: () => void;
  loading?: boolean;
  defaultExpanded?: boolean;
};

const inputClass =
  "w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:ring-2 focus:ring-brand-600/25";

function countActiveFilters(values: FilterValues): number {
  return Object.values(values).filter((v) => v.trim() !== "").length;
}

export function FilterBar({
  fields,
  values,
  onChange,
  onApply,
  onReset,
  loading,
  defaultExpanded = false,
}: Props) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const activeCount = useMemo(() => countActiveFilters(values), [values]);

  return (
    <div className="mt-4 overflow-hidden rounded-xl border border-stone-200 bg-stone-50/50">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        aria-expanded={expanded}
        className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left transition hover:bg-stone-100/60"
      >
        <span className="flex min-w-0 items-center gap-2">
          <span className="text-sm font-semibold text-stone-800">Bộ lọc</span>
          {activeCount > 0 ? (
            <span className="rounded-full bg-brand-100 px-2 py-0.5 text-xs font-medium text-brand-900">
              {activeCount} điều kiện
            </span>
          ) : (
            <span className="text-xs text-muted">Nhấn để {expanded ? "thu gọn" : "mở rộng"}</span>
          )}
        </span>
        <ChevronIcon open={expanded} />
      </button>

      {expanded ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            onApply();
          }}
          className="border-t border-stone-200 px-4 pb-4 pt-3"
        >
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {fields.map((field) => (
              <label key={field.key} className="block text-xs font-medium text-stone-600">
                {field.label}
                {field.type === "select" ? (
                  <select
                    value={values[field.key] ?? ""}
                    onChange={(e) => onChange(field.key, e.target.value)}
                    className={`${inputClass} mt-1`}
                  >
                    <option value="">{field.emptyOption ?? "— Tất cả —"}</option>
                    {field.options.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type={field.type === "number" ? "number" : "text"}
                    value={values[field.key] ?? ""}
                    onChange={(e) => onChange(field.key, e.target.value)}
                    placeholder={field.placeholder}
                    min={field.type === "number" ? 1 : undefined}
                    className={`${inputClass} mt-1`}
                  />
                )}
              </label>
            ))}
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            <button
              type="submit"
              disabled={loading}
              className="rounded-lg bg-brand-800 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50"
            >
              Áp dụng
            </button>
            <button
              type="button"
              disabled={loading}
              onClick={onReset}
              className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium text-stone-700 hover:bg-stone-100 disabled:opacity-50"
            >
              Xóa lọc
            </button>
          </div>
        </form>
      ) : null}
    </div>
  );
}

function ChevronIcon({ open }: { open: boolean }) {
  return (
    <svg
      className={`h-5 w-5 shrink-0 text-stone-500 transition-transform ${open ? "rotate-180" : ""}`}
      viewBox="0 0 20 20"
      fill="currentColor"
      aria-hidden
    >
      <path
        fillRule="evenodd"
        d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.24 4.5a.75.75 0 01-1.08 0l-4.24-4.5a.75.75 0 01.02-1.06z"
        clipRule="evenodd"
      />
    </svg>
  );
}
