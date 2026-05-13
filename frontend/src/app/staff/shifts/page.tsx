"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffShiftCreateDialog } from "@/components/staff/StaffShiftCreateDialog";
import { StaffShiftEditDialog } from "@/components/staff/StaffShiftEditDialog";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { PaginatedResponse, ShiftModel } from "@/lib/api/types";

const STATUS_LABEL: Record<string, string> = {
  SCHEDULED: "Đã đặt ca",
  IN_PROGRESS: "Đang diễn ra",
  COMPLETED: "Đã hoàn thành",
  CANCELLED: "Đã hủy",
};

export default function StaffShiftsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [rows, setRows] = useState<ShiftModel[]>([]);
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<ShiftModel | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const canManage = hasRole("ADMIN", "MANAGER");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<ShiftModel>>(`/shifts/filters?${buildPageParams(0, 80)}`);
      setRows(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được ca làm");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/shifts");
      return;
    }
    if (!hasRole("ADMIN", "MANAGER", "WAITER", "CHEF", "CASHIER")) {
      router.replace("/staff");
      return;
    }
    void Promise.resolve().then(() => load());
  }, [user, authLoading, hasRole, router, load]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
          Ca làm
        </h1>
        {canManage ? (
          <button
            type="button"
            onClick={() => {
              setEditing(null);
              setCreating(true);
            }}
            className="shrink-0 rounded-lg border border-brand-200 bg-brand-50 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-brand-100"
          >
            Thêm ca
          </button>
        ) : null}
      </div>
      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl border border-stone-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-stone-100 text-stone-600">
              <tr>
                <th className="px-3 py-3 font-medium">NV #</th>
                <th className="px-3 py-3 font-medium">Ngày</th>
                <th className="px-3 py-3 font-medium">Bắt đầu</th>
                <th className="px-3 py-3 font-medium">Kết thúc</th>
                <th className="px-3 py-3 font-medium">Giờ</th>
                <th className="px-3 py-3 font-medium">Trạng thái</th>
                {canManage ? <th className="px-3 py-3 font-medium w-28">Thao tác</th> : null}
              </tr>
            </thead>
            <tbody>
              {rows.map((s) => (
                <tr key={s.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                  <td className="px-3 py-2.5 font-medium tabular-nums">{s.employeeId}</td>
                  <td className="px-3 py-2.5 text-xs text-muted">{s.shiftDate}</td>
                  <td className="px-3 py-2.5 text-xs">{s.startTime}</td>
                  <td className="px-3 py-2.5 text-xs">{s.endTime}</td>
                  <td className="px-3 py-2.5 tabular-nums">{s.totalWorkingHours ?? "—"}</td>
                  <td className="px-3 py-2.5">{STATUS_LABEL[s.shiftStatus] ?? s.shiftStatus}</td>
                  {canManage ? (
                    <td className="px-3 py-2.5">
                      <button
                        type="button"
                        onClick={() => {
                          setCreating(false);
                          setEditing(s);
                        }}
                        className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-900 hover:bg-brand-100"
                      >
                        Sửa
                      </button>
                    </td>
                  ) : null}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {canManage ? (
        <>
          <StaffShiftCreateDialog open={creating} onClose={() => setCreating(false)} onSaved={() => void load()} />
          <StaffShiftEditDialog
            open={editing != null}
            row={editing}
            onClose={() => setEditing(null)}
            onSaved={() => void load()}
          />
        </>
      ) : null}
    </div>
  );
}
