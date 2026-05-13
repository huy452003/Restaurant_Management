"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffMenuItemCreateDialog } from "@/components/staff/StaffMenuItemCreateDialog";
import { StaffMenuItemEditDialog } from "@/components/staff/StaffMenuItemEditDialog";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { MenuItemModel, PaginatedResponse } from "@/lib/api/types";
import { formatVnd } from "@/lib/money";

export default function StaffMenuItemsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [rows, setRows] = useState<MenuItemModel[]>([]);
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<MenuItemModel | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<MenuItemModel>>(`/menu-items/filters?${buildPageParams(0, 80)}`);
      setRows(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được món");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

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
    void Promise.resolve().then(() => load());
  }, [user, authLoading, hasRole, router, load]);

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
      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
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
                  <td className="px-3 py-2.5">{m.menuItemStatus}</td>
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
      )}

      <StaffMenuItemCreateDialog open={creating} onClose={() => setCreating(false)} onSaved={() => void load()} />
      <StaffMenuItemEditDialog
        open={editing != null}
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={() => void load()}
      />
    </div>
  );
}
