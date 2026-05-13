"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import {
  formatShiftDateTimeToApi,
  formatShiftDateToApi,
} from "@/lib/shiftApiFormat";
import type { PaginatedResponse, ShiftModel, UserModel, UserRole } from "@/lib/api/types";

const ELIGIBLE_ROLES: UserRole[] = ["MANAGER", "WAITER", "CHEF", "CASHIER"];

type Props = {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

function todayYyyyMmDd(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function StaffShiftCreateDialog({ open, onClose, onSaved }: Props) {
  const [employees, setEmployees] = useState<UserModel[]>([]);
  const [employeeId, setEmployeeId] = useState("");
  const [shiftDate, setShiftDate] = useState(todayYyyyMmDd());
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("17:00");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!open) return;
    void (async () => {
      setError(null);
      setShiftDate(todayYyyyMmDd());
      setStartTime("09:00");
      setEndTime("17:00");
      setNotes("");
      try {
        const res = await apiFetch<PaginatedResponse<UserModel>>(
          `/users/filterAndPaginate?${buildPageParams(0, 200)}`,
        );
        const list = (res.data.content ?? []).filter((u) => ELIGIBLE_ROLES.includes(u.role));
        setEmployees(list);
        setEmployeeId(list[0] ? String(list[0].id) : "");
      } catch {
        setEmployees([]);
        setEmployeeId("");
      }
    })();
  }, [open]);

  if (!open) return null;

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
      const payload = [
        {
          employeeId: eid,
          shiftDate: dateApi,
          startTime: startApi,
          endTime: endApi,
          shiftStatus: "SCHEDULED",
          ...(noteTrim ? { notes: noteTrim } : {}),
        },
      ];
      await apiFetch<ShiftModel[]>("/shifts", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Tạo ca thất bại");
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
        aria-labelledby="create-shift-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="create-shift-title" className="font-serif text-xl font-semibold text-brand-900">
          Thêm ca làm
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
              disabled={employees.length === 0}
            >
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
              disabled={pending || employees.length === 0}
              className="rounded-lg border border-brand-200 bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-50"
            >
              {pending ? "Đang lưu…" : "Tạo"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
