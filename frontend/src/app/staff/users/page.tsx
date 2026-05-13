"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffUserEditDialog } from "@/components/staff/StaffUserEditDialog";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { PaginatedResponse, UserModel } from "@/lib/api/types";
import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";

const PAGE_SIZE = 15;

export default function StaffUsersPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [rows, setRows] = useState<UserModel[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<UserModel | null>(null);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    setError(null);
    try {
      const qs = buildPageParams(p, PAGE_SIZE);
      const res = await apiFetch<PaginatedResponse<UserModel>>(`/users/filterAndPaginate?${qs}`);
      setRows(res.data.content ?? []);
      setTotalPages(res.data.totalPages ?? 0);
      setTotalElements(Number(res.data.totalElements ?? 0));
      setPage(res.data.page ?? p);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được danh sách");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/users");
      return;
    }
    if (!hasRole("ADMIN")) {
      router.replace("/staff");
      return;
    }
    void Promise.resolve().then(() => load(0));
  }, [user, authLoading, hasRole, router, load]);

  if (authLoading || !user || !hasRole("ADMIN")) {
    return <div className="py-20 text-center text-muted">Đang tải…</div>;
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Người dùng
      </h1>

      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}

      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <>
          <p className="mt-4 text-sm text-muted">
            Tổng <span className="font-medium text-stone-800">{totalElements}</span> tài khoản
          </p>
          <div className="mt-4 overflow-x-auto rounded-xl border border-stone-200">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-stone-100 text-stone-600">
                <tr>
                  <th className="px-3 py-3 font-medium">ID</th>
                  <th className="px-3 py-3 font-medium">Tên đăng nhập</th>
                  <th className="px-3 py-3 font-medium">Họ tên</th>
                  <th className="px-3 py-3 font-medium">Email</th>
                  <th className="px-3 py-3 font-medium">SĐT</th>
                  <th className="px-3 py-3 font-medium">Vai trò</th>
                  <th className="px-3 py-3 font-medium">Trạng thái</th>
                  <th className="px-3 py-3 w-24 font-medium text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((u) => (
                  <tr key={u.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                    <td className="px-3 py-2.5">{u.id}</td>
                    <td className="px-3 py-2.5 font-mono text-xs">{u.username}</td>
                    <td className="px-3 py-2.5">{u.fullname}</td>
                    <td className="px-3 py-2.5 text-muted">{u.email}</td>
                    <td className="px-3 py-2.5">{u.phone}</td>
                    <td className="px-3 py-2.5">{STAFF_ROLE_LABEL_VI[u.role]}</td>
                    <td className="px-3 py-2.5">{u.userStatus}</td>
                    <td className="px-3 py-2.5 text-right">
                      <button
                        type="button"
                        onClick={() => setEditing(u)}
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
          {totalPages > 1 ? (
            <div className="mt-4 flex flex-wrap items-center gap-3">
              <button
                type="button"
                disabled={page <= 0 || loading}
                onClick={() => void load(page - 1)}
                className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium hover:bg-stone-50 disabled:opacity-40"
              >
                Trang trước
              </button>
              <span className="text-sm text-muted">
                Trang {page + 1} / {totalPages}
              </span>
              <button
                type="button"
                disabled={page >= totalPages - 1 || loading}
                onClick={() => void load(page + 1)}
                className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium hover:bg-stone-50 disabled:opacity-40"
              >
                Trang sau
              </button>
            </div>
          ) : null}
        </>
      )}

      <StaffUserEditDialog
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={() => void load(page)}
      />
    </div>
  );
}
