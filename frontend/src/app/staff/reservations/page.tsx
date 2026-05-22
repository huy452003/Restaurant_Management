"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { ReservationDetailDialog } from "@/components/reservations/ReservationDetailDialog";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { StaffReservationEditDialog } from "@/components/staff/StaffReservationEditDialog";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import { ApiError } from "@/lib/api/client";
import type { ReservationModel } from "@/lib/api/types";
import { LIST_EXTRA_SORT_NEWEST, RESERVATION_ADMIN_LIST_FILTERS } from "@/lib/list/presets";
import {
  isReservationAdminEditable,
  isReservationCancellable,
} from "@/lib/reservations/labels";
import { cancelReservationAsStaff } from "@/lib/reservations/staff-actions";
import { formatReservationSchedule } from "@/lib/reservations/slots";

const STAFF_RESERVATION_ROLES = ["ADMIN", "MANAGER", "CASHIER"] as const;

export default function StaffReservationsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [actionError, setActionError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<number | null>(null);
  const [detail, setDetail] = useState<ReservationModel | null>(null);
  const [editing, setEditing] = useState<ReservationModel | null>(null);

  const canStaffUpdate = hasRole("ADMIN", "MANAGER", "CASHIER");

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
    goToPage,
    loadInitial,
    reload,
  } = usePaginatedList<ReservationModel>({
    basePath: "/reservations/filters/admin",
    pageSize: 15,
    extraParams: LIST_EXTRA_SORT_NEWEST,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/reservations");
      return;
    }
    if (!hasRole(...STAFF_RESERVATION_ROLES)) {
      router.replace("/staff");
      return;
    }
    loadInitial();
  }, [user, authLoading, hasRole, router, loadInitial]);

  const cancelReservation = useCallback(
    async (id: number) => {
      if (!window.confirm("Hủy đặt bàn này?")) return;
      setActionError(null);
      setActingId(id);
      try {
        await cancelReservationAsStaff(id);
        reload();
      } catch (e) {
        setActionError(e instanceof ApiError ? e.message : "Hủy thất bại");
      } finally {
        setActingId(null);
      }
    },
    [reload],
  );

  function openEdit(r: ReservationModel) {
    setDetail(null);
    setEditing(r);
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1
        className="font-serif text-2xl font-semibold text-brand-900"
        style={{ fontFamily: "var(--font-cormorant), serif" }}
      >
        Đặt chỗ
      </h1>
      <p className="mt-1 text-sm text-muted">
        Xác nhận, hoàn tất hoặc hủy qua Sửa (đổi trạng thái trong form).
      </p>

      <FilterBar
        fields={RESERVATION_ADMIN_LIST_FILTERS}
        values={draftFilters}
        onChange={setFilter}
        onApply={applyFilters}
        onReset={resetFilters}
        loading={loading}
      />

      {error ? (
        <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p>
      ) : null}
      {actionError ? (
        <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{actionError}</p>
      ) : null}
      {loading && rows.length === 0 ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <>
          <div className="mt-6 overflow-x-auto rounded-xl border border-stone-200">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-stone-100 text-stone-600">
                <tr>
                  <th className="px-3 py-3 font-medium">#</th>
                  <th className="px-3 py-3 font-medium">Khách</th>
                  <th className="px-3 py-3 font-medium">SĐT</th>
                  <th className="px-3 py-3 font-medium">Bàn</th>
                  <th className="px-3 py-3 font-medium">Giờ</th>
                  <th className="px-3 py-3 font-medium">Số khách</th>
                  <th className="px-3 py-3 font-medium">Trạng thái</th>
                  <th className="px-3 py-3 font-medium min-w-[9rem]">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => {
                  const editable =
                    canStaffUpdate && isReservationAdminEditable(r.reservationStatus);
                  const cancellable = isReservationCancellable(r.reservationStatus);
                  const busy = actingId === r.id;
                  return (
                    <tr
                      key={r.id}
                      className="border-t border-stone-100 bg-surface hover:bg-stone-50/80"
                    >
                      <td className="px-3 py-2.5 text-muted">{r.id}</td>
                      <td className="px-3 py-2.5">{r.customerName ?? "—"}</td>
                      <td className="px-3 py-2.5 text-muted">{r.customerPhone ?? "—"}</td>
                      <td className="px-3 py-2.5">{r.tableNumber}</td>
                      <td className="px-3 py-2.5 text-xs">{formatReservationSchedule(r)}</td>
                      <td className="px-3 py-2.5">{r.numberOfGuests}</td>
                      <td className="px-3 py-2.5">
                        <StatusBadge domain="reservation" status={r.reservationStatus} />
                      </td>
                      <td className="px-3 py-2.5">
                        <div className="flex flex-wrap gap-1.5">
                          <button
                            type="button"
                            onClick={() => setDetail(r)}
                            className="rounded-lg border border-stone-300 px-2.5 py-1 text-xs font-medium text-stone-700 hover:bg-stone-50"
                          >
                            Chi tiết
                          </button>
                          {editable ? (
                            <button
                              type="button"
                              onClick={() => openEdit(r)}
                              className="rounded-lg border border-brand-200 bg-brand-50 px-2.5 py-1 text-xs font-medium text-brand-900 hover:bg-brand-100"
                            >
                              Sửa
                            </button>
                          ) : null}
                          {cancellable ? (
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => void cancelReservation(r.id)}
                              className="rounded-lg border border-stone-300 px-2.5 py-1 text-xs font-medium text-stone-700 hover:bg-stone-50 disabled:opacity-50"
                            >
                              {busy ? "…" : "Hủy"}
                            </button>
                          ) : null}
                        </div>
                      </td>
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
            unitLabel="đặt chỗ"
          />
        </>
      )}

      <ReservationDetailDialog
        open={detail != null}
        reservation={detail}
        onClose={() => setDetail(null)}
        onEdit={
          detail && canStaffUpdate && isReservationAdminEditable(detail.reservationStatus)
            ? () => openEdit(detail)
            : undefined
        }
      />

      <StaffReservationEditDialog
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={() => {
          reload();
          setDetail(null);
        }}
      />
    </div>
  );
}
