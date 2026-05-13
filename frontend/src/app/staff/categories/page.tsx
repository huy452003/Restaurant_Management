"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffCategoryDialog } from "@/components/staff/StaffCategoryDialog";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { CategoryModel, PaginatedResponse } from "@/lib/api/types";

const STATUS_LABEL: Record<string, string> = {
  AVAILABLE: "Có sẵn",
  OUT_OF_STOCK: "Tạm hết hàng",
  DISCONTINUED: "Ngừng kinh doanh",
};

export default function StaffCategoriesPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [rows, setRows] = useState<CategoryModel[]>([]);
  const [editing, setEditing] = useState<CategoryModel | null>(null);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<CategoryModel>>(`/categories/filters?${buildPageParams(0, 80)}`);
      setRows(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được danh mục");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

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
    void Promise.resolve().then(() => load());
  }, [user, authLoading, hasRole, router, load]);

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
      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
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
                  <td className="px-3 py-2.5">{STATUS_LABEL[c.categoryStatus] ?? c.categoryStatus}</td>
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
      )}

      <StaffCategoryDialog
        open={dialogOpen}
        mode={creating ? "create" : "edit"}
        row={editing}
        onClose={closeDialog}
        onSaved={() => void load()}
      />
    </div>
  );
}
