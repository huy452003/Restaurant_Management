"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import {
  formatShiftDateTimeToApi,
  formatShiftDateToApi,
  parseApiShiftDateTimeToInput,
  parseApiShiftDateToInput,
} from "@/lib/shiftApiFormat";
import type { PaginatedResponse, ShiftModel, UserModel, UserRole } from "@/lib/api/types";

const ELIGIBLE_ROLES: UserRole[] = ["MANAGER", "WAITER", "CHEF", "CASHIER"];

const SHIFT_STATUSES = ["SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"] as const;
type ShiftStatusEdit = (typeof SHIFT_STATUSES)[number];

const STATUS_LABEL: Record<ShiftStatusEdit, string> = {
  SCHEDULED: "Đã đặt ca",
  IN_PROGRESS: "Đang diễn ra",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
};

type Props = {
  open: boolean;
  row: ShiftModel | null;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

export function StaffShiftEditDialog({ open, row, onClose, onSaved }: Props) {
  const [employees, setEmployees] = useState<UserModel[]>([]);
  const [employeeId, setEmployeeId] = useState("");
  const [shiftDate, setShiftDate] = useState("");
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("17:00");
  const [shiftStatus, setShiftStatus] = useState<ShiftStatusEdit>("SCHEDULED");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!open || !row) return;
    void (async () => {
      setError(null);
      try {
        const res = await apiFetch<PaginatedResponse<UserModel>>(
          `/users/filterAndPaginate?${buildPageParams(0, 200)}`,
        );
        const list = (res.data.content ?? []).filter((u) => ELIGIBLE_ROLES.includes(u.role));
        setEmployees(list);
      } catch {
        setEmployees([]);
      }

      const dateFromRow = parseApiShiftDateToInput(row.shiftDate);
      const st = parseApiShiftDateTimeToInput(row.startTime);
      const en = parseApiShiftDateTimeToInput(row.endTime);
      const date = dateFromRow ?? st?.date ?? en?.date ?? "";
      setShiftDate(date);
      setStartTime(st?.time ?? "09:00");
      setEndTime(en?.time ?? "17:00");

      setEmployeeId(String(row.employeeId));
      setShiftStatus(
        (SHIFT_STATUSES as readonly string[]).includes(row.shiftStatus)
          ? (row.shiftStatus as ShiftStatusEdit)
          : "SCHEDULED",
      );
      setNotes(row.notes ?? "");
    })();
  }, [open, row]);

  if (!open || !row) return null;

  const editingRow = row;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const eid = Number(employeeId);
    if (!Number.isInteger(eid) || eid < 1) {
      setError("Chọn nhân viên");
      return;
    }
    if (!shiftDate) {
      setError("Chọn ngày ca");
      return;
    }
    const noteTrim = notes.trim().slice(0, 300);
    setPending(true);
    try {
      const dateApi = formatShiftDateToApi(shiftDate);
      const startApi = formatShiftDateTimeToApi(shiftDate, startTime);
      const endApi = formatShiftDateTimeToApi(shiftDate, endTime);
      const update = {
        employeeId: eid,
        shiftDate: dateApi,
        startTime: startApi,
        endTime: endApi,
        shiftStatus,
        ...(noteTrim ? { notes: noteTrim } : {}),
      };
      await apiFetch<ShiftModel[]>("/shifts", {
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

  const orphanEmployee = !employees.some((u) => u.id === editingRow.employeeId);

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
        aria-labelledby="edit-shift-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="edit-shift-title" className="font-serif text-xl font-semibold text-brand-900">
          Sửa ca{" "}
          <span className="font-mono text-[1.0625rem] font-semibold tabular-nums tracking-normal text-brand-900">
            #{editingRow.id}
          </span>
        </h2>

        <form onSubmit={handleSubmit} className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          <label className="block text-xs font-medium text-stone-600">
            Nhân viên
            <select
              className={fieldClass}
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              required
            >
              {orphanEmployee ? (
                <option value={String(editingRow.employeeId)}>#{editingRow.employeeId} (không trong danh sách)</option>
              ) : null}
              {employees.map((u) => (
                <option key={u.id} value={String(u.id)}>
                  #{u.id} · {u.fullname} ({u.role})
                </option>
              ))}
            </select>
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Ngày ca
            <input
              className={fieldClass}
              type="date"
              value={shiftDate}
              onChange={(e) => setShiftDate(e.target.value)}
              required
            />
          </label>

          <div className="grid grid-cols-2 gap-3">
            <label className="block text-xs font-medium text-stone-600">
              Giờ bắt đầu
              <input className={fieldClass} type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} required />
            </label>
            <label className="block text-xs font-medium text-stone-600">
              Giờ kết thúc
              <input className={fieldClass} type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} required />
            </label>
          </div>

          <label className="block text-xs font-medium text-stone-600">
            Trạng thái
            <select
              className={fieldClass}
              value={shiftStatus}
              onChange={(e) => setShiftStatus(e.target.value as ShiftStatusEdit)}
              required
            >
              {SHIFT_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABEL[s]}
                </option>
              ))}
            </select>
          </label>

          <label className="block text-xs font-medium text-stone-600">
            Ghi chú (tối đa 300 ký tự)
            <textarea
              className={`${fieldClass} min-h-[72px] resize-y`}
              value={notes}
              onChange={(e) => setNotes(e.target.value.slice(0, 300))}
              maxLength={300}
              rows={2}
            />
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
              disabled={pending || !shiftDate}
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
