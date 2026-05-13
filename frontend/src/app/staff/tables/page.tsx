"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffTableCreateDialog } from "@/components/staff/StaffTableCreateDialog";
import { StaffTableEditDialog } from "@/components/staff/StaffTableEditDialog";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { PaginatedResponse, TableModel } from "@/lib/api/types";

const STATUS_LABEL: Record<string, string> = {
  AVAILABLE: "Trống",
  OCCUPIED: "Đang phục vụ",
  RESERVED: "Đã đặt",
  CLEANING: "Đang dọn",
};

export default function StaffTablesPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [rows, setRows] = useState<TableModel[]>([]);
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<TableModel | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<TableModel>>(`/tables/filters?${buildPageParams(0, 80)}`);
      setRows(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được bàn");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/tables");
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
          Bàn
        </h1>
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
      </div>
      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl border border-stone-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-stone-100 text-stone-600">
              <tr>
                <th className="px-3 py-3 font-medium">Số bàn</th>
                <th className="px-3 py-3 font-medium">Sức chứa</th>
                <th className="px-3 py-3 font-medium">Vị trí</th>
                <th className="px-3 py-3 font-medium">Trạng thái</th>
                <th className="px-3 py-3 font-medium w-28">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((t) => (
                <tr key={t.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                  <td className="px-3 py-2.5 font-medium tabular-nums">{t.tableNumber}</td>
                  <td className="px-3 py-2.5 tabular-nums">{t.capacity ?? "—"}</td>
                  <td className="max-w-[240px] px-3 py-2.5 text-muted">
                    {t.location ? <span className="line-clamp-2 break-words">{t.location}</span> : "—"}
                  </td>
                  <td className="px-3 py-2.5">{STATUS_LABEL[t.tableStatus] ?? t.tableStatus}</td>
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
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <StaffTableCreateDialog open={creating} onClose={() => setCreating(false)} onSaved={() => void load()} />
      <StaffTableEditDialog
        open={editing != null}
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={() => void load()}
      />
    </div>
  );
}
