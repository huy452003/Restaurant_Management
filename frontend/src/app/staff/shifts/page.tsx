"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffShiftCreateDialog } from "@/components/staff/StaffShiftCreateDialog";
import { StaffShiftEditDialog } from "@/components/staff/StaffShiftEditDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import type { ShiftModel } from "@/lib/api/types";
import { LIST_EXTRA_SORT_NEWEST, SHIFT_LIST_FILTERS } from "@/lib/list/presets";
import { StatusBadge } from "@/components/ui/StatusBadge";

export default function StaffShiftsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<ShiftModel | null>(null);

  const canManage = hasRole("ADMIN", "MANAGER");

  const {
    rows,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    draftFilters,
    setFilter,
    applyFilters,
    resetFilters,
    reload,
    goToPage,
    loadInitial,
  } = usePaginatedList<ShiftModel>({
    basePath: "/shifts/filters",
    pageSize: 15,
    extraParams: LIST_EXTRA_SORT_NEWEST,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/shifts");
      return;
    }
    if (!hasRole("ADMIN", "MANAGER", "CHEF", "CASHIER")) {
      router.replace("/staff");
      return;
    }
    loadInitial();
  }, [user, authLoading, hasRole, router, loadInitial]);

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
      <FilterBar
        fields={SHIFT_LIST_FILTERS}
        values={draftFilters}
        onChange={setFilter}
        onApply={applyFilters}
        onReset={resetFilters}
        loading={loading}
      />

      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading && rows.length === 0 ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <>
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
                  <td className="px-3 py-2.5">
                    <StatusBadge domain="shift" status={s.shiftStatus} />
                  </td>
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
        {rows.length === 0 ? <p className="mt-4 text-sm text-muted">Không có kết quả phù hợp.</p> : null}
        <PaginationBar
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          loading={loading}
          onPageChange={goToPage}
          unitLabel="ca"
        />
        </>
      )}

      {canManage ? (
        <>
          <StaffShiftCreateDialog open={creating} onClose={() => setCreating(false)} onSaved={reload} />
          <StaffShiftEditDialog
            open={editing != null}
            row={editing}
            onClose={() => setEditing(null)}
            onSaved={reload}
          />
        </>
      ) : null}
    </div>
  );
}
