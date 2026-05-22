"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FilterBar } from "@/components/list/FilterBar";
import { PaginationBar } from "@/components/list/PaginationBar";
import { CustomerReservationEditDialog } from "@/components/reservations/CustomerReservationEditDialog";
import { ReservationGuestsInput } from "@/components/reservations/ReservationGuestsInput";
import { ReservationTimeSlotPicker } from "@/components/reservations/ReservationTimeSlotPicker";
import { TableNumberSelect } from "@/components/TableNumberSelect";
import { useAuth } from "@/context/auth-context";
import { usePaginatedList } from "@/hooks/use-paginated-list";
import { useReservationSlots } from "@/hooks/use-reservation-slots";
import { useRestaurantTables } from "@/hooks/use-restaurant-tables";
import { apiFetch, ApiError } from "@/lib/api/client";
import type {
  ReservationCustomerCreateModel,
  ReservationModel,
} from "@/lib/api/types";
import { LIST_EXTRA_SORT_NEWEST, RESERVATION_LIST_FILTERS } from "@/lib/list/presets";
import { PageHeading } from "@/components/ui/PageHeading";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { btnPrimaryClass, cardClass, fontSerif } from "@/lib/ui/bakery";
import { isReservationEditable } from "@/lib/reservations/labels";
import { clampGuestsForTable, tableCapacityFor } from "@/lib/reservations/guests";
import {
  formatReservationSchedule,
  inputDateToDdMmYyyy,
  todayInputDateValue,
} from "@/lib/reservations/slots";

export default function ReservationsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [listActionError, setListActionError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [tableNumber, setTableNumber] = useState<number | "">("");
  const { tables, loading: tablesLoading, error: tablesError } = useRestaurantTables({
    tableStatus: "AVAILABLE",
    excludeTablesWithPendingOrder: true,
    enabled: !!user && hasRole("CUSTOMER"),
  });
  const [guests, setGuests] = useState(2);
  const [reservationDate, setReservationDate] = useState(todayInputDateValue);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [special, setSpecial] = useState("");
  const [editing, setEditing] = useState<ReservationModel | null>(null);
  const [createFormOpen, setCreateFormOpen] = useState(false);

  const {
    times: selectableTimes,
    loading: slotsLoading,
    error: slotsError,
    reload: reloadSlots,
  } = useReservationSlots(tableNumber, reservationDate);

  const {
    rows: list,
    page,
    totalPages,
    totalElements,
    loading,
    error: listError,
    draftFilters,
    setFilter,
    applyFilters,
    resetFilters,
    reload,
    goToPage,
    loadInitial,
  } = usePaginatedList<ReservationModel>({
    basePath: "/reservations/filters",
    pageSize: 10,
    extraParams: LIST_EXTRA_SORT_NEWEST,
  });

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/reservations");
      return;
    }
    if (!hasRole("CUSTOMER")) return;
    loadInitial();
  }, [user, authLoading, hasRole, router, loadInitial]);

  useEffect(() => {
    if (tables.length === 0) return;
    if (tableNumber === "" || !tables.some((t) => t.tableNumber === tableNumber)) {
      setTableNumber(tables[0].tableNumber);
    }
  }, [tables, tableNumber]);

  const tableCapacity = tableCapacityFor(tables, tableNumber);

  useEffect(() => {
    setSelectedTime(null);
  }, [tableNumber, reservationDate]);

  useEffect(() => {
    if (tableCapacity == null) return;
    setGuests((g) => clampGuestsForTable(g, tableCapacity));
  }, [tableNumber, tableCapacity]);

  useEffect(() => {
    if (!selectedTime) return;
    if (!selectableTimes.includes(selectedTime)) setSelectedTime(null);
  }, [selectableTimes, selectedTime]);

  async function createReservation(e: React.FormEvent) {
    e.preventDefault();
    if (tableNumber === "") {
      setFormError("Chọn bàn");
      return;
    }
    if (!reservationDate.trim()) {
      setFormError("Chọn ngày");
      return;
    }
    if (!selectedTime) {
      setFormError("Chọn khung giờ");
      return;
    }
    if (tableCapacity != null && guests > tableCapacity) {
      setFormError(`Bàn ${tableNumber} tối đa ${tableCapacity} khách`);
      return;
    }
    setFormError(null);
    setPending(true);
    try {
      const payload: ReservationCustomerCreateModel = {
        tableNumber,
        numberOfGuests: guests,
        reservationDate: inputDateToDdMmYyyy(reservationDate),
        reservationTime: selectedTime,
        specialRequest: special.trim() || undefined,
      };
      await apiFetch<ReservationModel[]>("/reservations", {
        method: "POST",
        body: JSON.stringify([payload]),
      });
      setSpecial("");
      setSelectedTime(null);
      setCreateFormOpen(false);
      reload();
      void reloadSlots();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Tạo đặt bàn thất bại");
    } finally {
      setPending(false);
    }
  }

  async function cancelReservation(id: number) {
    setListActionError(null);
    try {
      await apiFetch<ReservationModel>(`/reservations/cancel/${id}`, { method: "PATCH" });
      reload();
    } catch (e) {
      setListActionError(e instanceof ApiError ? e.message : "Hủy thất bại");
    }
  }

  if (authLoading || !user) {
    return <div className="py-20 text-center text-muted">Đang tải…</div>;
  }

  if (!hasRole("CUSTOMER")) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center">
        <p className="text-muted">Trang đặt bàn dành cho khách. Bạn có thể vào mục Quản lý để xem lịch đặt của nhà hàng.</p>
        <Link href="/staff" className="mt-4 inline-block font-medium text-brand-800 underline">
          Về trang quản lý
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <PageHeading
        title="Đặt bàn"
        subtitle="Chọn bàn trống, khung giờ và số khách — giữ chỗ chỉ vài bước."
      />

      <section className={`mt-8 overflow-hidden ${cardClass}`}>
        <button
          type="button"
          onClick={() => setCreateFormOpen((v) => !v)}
          aria-expanded={createFormOpen}
          className="flex w-full items-center justify-between gap-3 px-5 py-4 text-left transition hover:bg-brand-50/80"
        >
          <span className="flex min-w-0 flex-col gap-0.5 sm:flex-row sm:items-center sm:gap-2">
            <span
              className="font-serif text-lg font-semibold text-brand-900"
              style={fontSerif}
            >
              Đặt bàn mới
            </span>
            <span className="text-xs text-muted">
              {createFormOpen ? "Thu gọn form" : "Mở form đặt chỗ"}
            </span>
          </span>
          <CreateFormChevron open={createFormOpen} />
        </button>

        {createFormOpen ? (
      <form
        onSubmit={createReservation}
        className="grid gap-4 border-t border-stone-200 p-6 sm:grid-cols-2"
      >
        {formError ? (
          <p className="sm:col-span-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{formError}</p>
        ) : null}
        <TableNumberSelect
          id="reservation-table"
          label="Số bàn mong muốn"
          value={tableNumber}
          onChange={setTableNumber}
          tables={tables}
          loading={tablesLoading}
          error={tablesError}
          emptyHint="Hiện không có bàn trống. Vui lòng chọn thời gian khác hoặc liên hệ nhà hàng."
        />
        <div>
          <ReservationGuestsInput
            value={guests}
            onChange={setGuests}
            capacity={tableCapacity}
            disabled={tableNumber === ""}
          />
        </div>
        <div>
          <label htmlFor="reservation-date" className="block text-sm font-medium text-stone-700">
            Ngày
          </label>
          <input
            id="reservation-date"
            type="date"
            value={reservationDate}
            min={todayInputDateValue()}
            onChange={(e) => setReservationDate(e.target.value)}
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm"
            required
          />
        </div>
        <ReservationTimeSlotPicker
          times={selectableTimes}
          loading={slotsLoading}
          error={slotsError}
          selectedTime={selectedTime}
          onSelectTime={setSelectedTime}
          disabled={tableNumber === ""}
        />
        <div className="sm:col-span-2">
          <label className="block text-sm font-medium text-stone-700">Yêu cầu đặc biệt</label>
          <textarea
            value={special}
            onChange={(e) => setSpecial(e.target.value)}
            maxLength={300}
            rows={2}
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm"
          />
        </div>
        <div className="sm:col-span-2">
          <button
            type="submit"
            disabled={
              pending ||
              tableNumber === "" ||
              tables.length === 0 ||
              !selectedTime ||
              (tableCapacity != null && guests > tableCapacity)
            }
            className={`${btnPrimaryClass} disabled:opacity-50`}
          >
            {pending ? "Đang gửi…" : "Gửi yêu cầu đặt bàn"}
          </button>
        </div>
      </form>
        ) : null}
      </section>

      <h2 className="mt-10 font-serif text-xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Lịch của bạn
      </h2>

      <FilterBar
        fields={RESERVATION_LIST_FILTERS}
        values={draftFilters}
        onChange={setFilter}
        onApply={applyFilters}
        onReset={resetFilters}
        loading={loading}
      />

      {listError || listActionError ? (
        <p className="mt-4 text-sm text-red-700">{listActionError ?? listError}</p>
      ) : null}

      {loading && list.length === 0 ? (
        <p className="mt-4 text-muted">Đang tải…</p>
      ) : list.length === 0 ? (
        <p className="mt-4 text-sm text-muted">Chưa có lịch đặt.</p>
      ) : (
        <>
        <ul className="mt-4 space-y-3">
          {list.map((r) => {
            const editable = isReservationEditable(r.reservationStatus);
            return (
              <li
                key={r.id}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-stone-200 bg-surface px-4 py-3.5 text-sm shadow-sm"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-medium text-stone-900">
                      Bàn {r.tableNumber} · {r.numberOfGuests} khách
                    </p>
                    <StatusBadge domain="reservation" status={r.reservationStatus} />
                  </div>
                  <p className="mt-0.5 text-muted">{formatReservationSchedule(r)}</p>
                  {r.specialRequest?.trim() ? (
                    <p className="mt-1 line-clamp-2 text-xs text-stone-500">{r.specialRequest}</p>
                  ) : null}
                </div>
                <div className="flex shrink-0 flex-wrap gap-2">
                  {editable ? (
                    <button
                      type="button"
                      onClick={() => setEditing(r)}
                      className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-900 hover:bg-brand-100"
                    >
                      Sửa
                    </button>
                  ) : null}
                  {editable ? (
                    <button
                      type="button"
                      onClick={() => void cancelReservation(r.id)}
                      className="rounded-lg border border-stone-300 px-3 py-1.5 text-xs font-medium text-stone-700 hover:bg-stone-50"
                    >
                      Hủy
                    </button>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ul>
        <PaginationBar
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          loading={loading}
          onPageChange={goToPage}
          unitLabel="lịch đặt"
        />
        </>
      )}

      <CustomerReservationEditDialog
        open={editing != null}
        reservation={editing}
        onClose={() => setEditing(null)}
        onSaved={() => {
          reload();
          void reloadSlots();
        }}
      />
    </div>
  );
}

function CreateFormChevron({ open }: { open: boolean }) {
  return (
    <svg
      className={`h-5 w-5 shrink-0 text-stone-500 transition-transform ${open ? "rotate-180" : ""}`}
      viewBox="0 0 20 20"
      fill="currentColor"
      aria-hidden
    >
      <path
        fillRule="evenodd"
        d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.24 4.5a.75.75 0 01-1.08 0l-4.24-4.5a.75.75 0 01.02-1.06z"
        clipRule="evenodd"
      />
    </svg>
  );
}
