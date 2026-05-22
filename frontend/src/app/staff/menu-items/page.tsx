"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffMenuItemCreateDialog } from "@/components/staff/StaffMenuItemCreateDialog";
import { StaffMenuItemEditDialog } from "@/components/staff/StaffMenuItemEditDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import type { MenuItemModel } from "@/lib/api/types";
import { MENU_ITEM_LIST_FILTERS } from "@/lib/list/presets";
import { formatVnd } from "@/lib/money";
import { StatusBadge } from "@/components/ui/StatusBadge";

export default function StaffMenuItemsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<MenuItemModel | null>(null);

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
  } = usePaginatedList<MenuItemModel>({
    basePath: "/menu-items/filters",
    pageSize: 10,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/menu-items");
      return;
    }
    if (!hasRole("ADMIN", "MANAGER")) {
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
          Món ăn
        </h1>
        <button
          type="button"
          onClick={() => {
            setEditing(null);
            setCreating(true);
          }}
          className="shrink-0 rounded-lg border border-brand-200 bg-brand-50 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-brand-100"
        >
          Thêm món
        </button>
      </div>
      <FilterBar
        fields={MENU_ITEM_LIST_FILTERS}
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
                <th className="px-3 py-3 font-medium">Tên món</th>
                <th className="px-3 py-3 font-medium">Danh mục</th>
                <th className="px-3 py-3 font-medium">Giá</th>
                <th className="px-3 py-3 font-medium">Trạng thái</th>
                <th className="px-3 py-3 font-medium w-28">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((m) => (
                <tr key={m.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                  <td className="px-3 py-2.5 font-medium">{m.name}</td>
                  <td className="px-3 py-2.5 text-muted">{m.categoryName}</td>
                  <td className="px-3 py-2.5">{formatVnd(m.price)}</td>
                  <td className="px-3 py-2.5">
                    <StatusBadge domain="menuItem" status={m.menuItemStatus} />
                  </td>
                  <td className="px-3 py-2.5">
                    <button
                      type="button"
                      onClick={() => {
                        setCreating(false);
                        setEditing(m);
                      }}
                      className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-semibold text-brand-900 hover:bg-brand-100"
                    >
                      Sửa
                    </button>
                  </td>
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
          unitLabel="món"
        />
        </>
      )}

      <StaffMenuItemCreateDialog open={creating} onClose={() => setCreating(false)} onSaved={reload} />
      <StaffMenuItemEditDialog
        open={editing != null}
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={reload}
      />
    </div>
  );
}
