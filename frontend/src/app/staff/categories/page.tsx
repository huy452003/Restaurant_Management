"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffCategoryDialog } from "@/components/staff/StaffCategoryDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import type { CategoryModel } from "@/lib/api/types";
import { CATEGORY_LIST_FILTERS } from "@/lib/list/presets";
import { StatusBadge } from "@/components/ui/StatusBadge";

export default function StaffCategoriesPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [editing, setEditing] = useState<CategoryModel | null>(null);
  const [creating, setCreating] = useState(false);

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
  } = usePaginatedList<CategoryModel>({
    basePath: "/categories/filters",
    pageSize: 10,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/categories");
      return;
    }
    if (!hasRole("ADMIN", "MANAGER")) {
      router.replace("/staff");
      return;
    }
    loadInitial();
  }, [user, authLoading, hasRole, router, loadInitial]);

  const dialogOpen = creating || editing != null;

  function closeDialog() {
    setCreating(false);
    setEditing(null);
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
          Danh mục
        </h1>
        <button
          type="button"
          onClick={() => {
            setEditing(null);
            setCreating(true);
          }}
          className="shrink-0 rounded-lg border border-brand-200 bg-brand-50 px-4 py-2 text-sm font-semibold text-brand-900 hover:bg-brand-100"
        >
          Thêm danh mục
        </button>
      </div>
      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      <FilterBar
        fields={CATEGORY_LIST_FILTERS}
        values={draftFilters}
        onChange={setFilter}
        onApply={applyFilters}
        onReset={resetFilters}
        loading={loading}
      />

      {loading && rows.length === 0 ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <>
        <div className="mt-6 overflow-x-auto rounded-xl border border-stone-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-stone-100 text-stone-600">
              <tr>
                <th className="px-3 py-3 font-medium">Tên</th>
                <th className="px-3 py-3 font-medium">Mô tả</th>
                <th className="px-3 py-3 font-medium">Ảnh</th>
                <th className="px-3 py-3 font-medium">Trạng thái</th>
                <th className="px-3 py-3 font-medium w-28">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((c) => (
                <tr key={c.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                  <td className="px-3 py-2.5 font-medium text-brand-900">{c.name}</td>
                  <td className="max-w-[220px] px-3 py-2.5 text-muted">
                    {c.description ? <span className="line-clamp-2">{c.description}</span> : "—"}
                  </td>
                  <td className="max-w-[180px] px-3 py-2.5">
                    <span className="line-clamp-2 break-all font-mono text-xs text-stone-600">{c.image}</span>
                  </td>
                  <td className="px-3 py-2.5">
                    <StatusBadge domain="category" status={c.categoryStatus} />
                  </td>
                  <td className="px-3 py-2.5">
                    <button
                      type="button"
                      onClick={() => {
                        setCreating(false);
                        setEditing(c);
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
          unitLabel="danh mục"
        />
        </>
      )}

      <StaffCategoryDialog
        open={dialogOpen}
        mode={creating ? "create" : "edit"}
        row={editing}
        onClose={closeDialog}
        onSaved={reload}
      />
    </div>
  );
}
