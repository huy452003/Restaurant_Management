"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { TableModel } from "@/lib/api/types";

type Props = {
  open: boolean;
  row: TableModel | null;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

function parsePositiveInt(raw: string, label: string): { ok: true; value: number } | { ok: false; message: string } {
  const t = raw.trim();
  if (!t) return { ok: false, message: `Nhập ${label}` };
  const n = Number(t);
  if (!Number.isInteger(n) || n < 1) return { ok: false, message: `${label} phải là số nguyên ≥ 1` };
  return { ok: true, value: n };
}

export function StaffTableEditDialog({ open, row, onClose, onSaved }: Props) {
  const [tableNumber, setTableNumber] = useState("");
  const [capacity, setCapacity] = useState("");
  const [location, setLocation] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!open || !row) return;
    setError(null);
    setTableNumber(String(row.tableNumber ?? ""));
    setCapacity(row.capacity != null ? String(row.capacity) : "4");
    setLocation(row.location ?? "");
  }, [open, row]);

  if (!open || !row) return null;

  const editingRow = row;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const numResult = parsePositiveInt(tableNumber, "Số bàn");
    if (!numResult.ok) {
      setError(numResult.message);
      return;
    }
    const capResult = parsePositiveInt(capacity, "Sức chứa");
    if (!capResult.ok) {
      setError(capResult.message);
      return;
    }
    const loc = location.trim();
    if (!loc) {
      setError("Vị trí không được để trống");
      return;
    }
    setPending(true);
    try {
      const update = {
        tableNumber: numResult.value,
        capacity: capResult.value,
        location: loc,
      };
      await apiFetch<TableModel[]>("/tables", {
        method: "PUT",
        body: JSON.stringify({ ids: [editingRow.id], updates: [update] }),
      });
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
        aria-labelledby="edit-table-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="edit-table-title" className="font-serif text-xl font-semibold text-brand-900">
          Sửa bàn{" "}
          <span className="font-mono text-[1.0625rem] font-semibold tabular-nums tracking-normal text-brand-900">
            #{editingRow.id}
          </span>
        </h2>

        <form onSubmit={handleSubmit} className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          <label className="block text-xs font-medium text-stone-600">
            Số bàn (số nguyên ≥ 1)
            <input
              className={fieldClass}
              type="number"
              min={1}
              step={1}
              inputMode="numeric"
              value={tableNumber}
              onChange={(e) => setTableNumber(e.target.value)}
              required
            />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Sức chứa (chỗ)
            <input
              className={fieldClass}
              type="number"
              min={1}
              step={1}
              inputMode="numeric"
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              required
            />
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Vị trí
            <input className={fieldClass} value={location} onChange={(e) => setLocation(e.target.value)} required />
          </label>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium text-stone-700 hover:bg-stone-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={pending}
              className="rounded-lg border border-brand-200 bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
            >
              {pending ? "Đang lưu…" : "Lưu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
