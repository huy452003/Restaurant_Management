"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffTableCreateDialog } from "@/components/staff/StaffTableCreateDialog";
import { StaffTableEditDialog } from "@/components/staff/StaffTableEditDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import type { TableModel, UserRole } from "@/lib/api/types";
import { TABLE_LIST_FILTERS } from "@/lib/list/presets";
import { StatusBadge } from "@/components/ui/StatusBadge";

function canViewTables(role: UserRole): boolean {
  return role === "ADMIN" || role === "MANAGER" || role === "CASHIER";
}

function canManageTables(role: UserRole): boolean {
  return role === "ADMIN" || role === "MANAGER";
}

export default function StaffTablesPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<TableModel | null>(null);

  const manageMode = user ? canManageTables(user.role) : false;
  const viewOnly = user ? canViewTables(user.role) && !manageMode : false;

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
  } = usePaginatedList<TableModel>({
    basePath: "/tables/filters",
    pageSize: 10,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/tables");
      return;
    }
    if (!canViewTables(user.role)) {
      router.replace("/staff");
      return;
    }
    loadInitial();
  }, [user, authLoading, router, loadInitial]);

  if (authLoading || !user) {
    return <div className="py-20 text-center text-muted">Đang tải…</div>;
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h1
            className="font-serif text-2xl font-semibold text-brand-900"
            style={{ fontFamily: "var(--font-cormorant), serif" }}
          >
            Bàn
          </h1>
          {viewOnly ? (
            <p className="mt-1 text-sm text-muted">
              Chế độ xem — theo dõi bàn trống để xếp khách ăn tại chỗ.
            </p>
          ) : null}
        </div>
        {manageMode ? (
          <button
            type="button"
            onClick={() => {
              setEditing(null);
              setCreating(true);
            }}
            className="shrink-0 rounded-lg border border-brand-200 bg-brand-50 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-brand-100"
          >
            Thêm bàn
          </button>
        ) : null}
      </div>

      <FilterBar
        fields={TABLE_LIST_FILTERS}
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
                  <th className="px-3 py-3 font-medium">Số bàn</th>
                  <th className="px-3 py-3 font-medium">Sức chứa</th>
                  <th className="px-3 py-3 font-medium">Vị trí</th>
                  <th className="px-3 py-3 font-medium">Trạng thái</th>
                  {manageMode ? <th className="w-28 px-3 py-3 font-medium">Thao tác</th> : null}
                </tr>
              </thead>
              <tbody>
                {rows.map((t) => {
                  return (
                    <tr key={t.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                      <td className="px-3 py-2.5 font-medium tabular-nums">{t.tableNumber}</td>
                      <td className="px-3 py-2.5 tabular-nums">{t.capacity ?? "—"}</td>
                      <td className="max-w-[240px] px-3 py-2.5 text-muted">
                        {t.location ? <span className="line-clamp-2 break-words">{t.location}</span> : "—"}
                      </td>
                      <td className="px-3 py-2.5">
                        <StatusBadge domain="table" status={t.tableStatus} />
                      </td>
                      {manageMode ? (
                        <td className="px-3 py-2.5">
                          <button
                            type="button"
                            onClick={() => {
                              setCreating(false);
                              setEditing(t);
                            }}
                            className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-900 hover:bg-brand-100"
                          >
                            Sửa
                          </button>
                        </td>
                      ) : null}
                    </tr>
                  );
                })}
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
            unitLabel="bàn"
          />
        </>
      )}

      {manageMode ? (
        <>
          <StaffTableCreateDialog open={creating} onClose={() => setCreating(false)} onSaved={reload} />
          <StaffTableEditDialog
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
