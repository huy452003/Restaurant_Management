"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffUserEditDialog } from "@/components/staff/StaffUserEditDialog";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import { StatusBadge } from "@/components/ui/StatusBadge";
import type { UserModel } from "@/lib/api/types";
import { USER_LIST_FILTERS } from "@/lib/list/presets";
import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";

export default function StaffUsersPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [editing, setEditing] = useState<UserModel | null>(null);

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
  } = usePaginatedList<UserModel>({
    basePath: "/users/filterAndPaginate",
    pageSize: 15,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/users");
      return;
    }
    if (user.role !== "ADMIN") {
      router.replace("/staff");
      return;
    }
    loadInitial();
  }, [user, authLoading, router, loadInitial]);

  if (authLoading || !user || !hasRole("ADMIN")) {
    return <div className="py-20 text-center text-muted">Đang tải…</div>;
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Người dùng
      </h1>

      <FilterBar
        fields={USER_LIST_FILTERS}
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
                    <td className="px-3 py-2.5">
                      <StatusBadge domain="user" status={u.userStatus} />
                    </td>
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
          {rows.length === 0 ? <p className="mt-4 text-sm text-muted">Không có kết quả phù hợp.</p> : null}
          <PaginationBar
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            loading={loading}
            onPageChange={goToPage}
            unitLabel="tài khoản"
          />
        </>
      )}

      <StaffUserEditDialog row={editing} onClose={() => setEditing(null)} onSaved={reload} />
    </div>
  );
}
