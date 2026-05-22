"use client";

import { useEffect, useMemo, useState } from "react";
import { ReservationGuestsInput } from "@/components/reservations/ReservationGuestsInput";
import { ReservationTimeSlotPicker } from "@/components/reservations/ReservationTimeSlotPicker";
import { TableNumberSelect } from "@/components/TableNumberSelect";
import { useReservationSlots } from "@/hooks/use-reservation-slots";
import { useRestaurantTables } from "@/hooks/use-restaurant-tables";
import { ApiError } from "@/lib/api/client";
import type { ReservationAdminRequestModel, ReservationModel, ReservationStatus } from "@/lib/api/types";
import { birthDdMmYyyyToInputDate } from "@/lib/dates";
import { clampGuestsForTable, tableCapacityFor } from "@/lib/reservations/guests";
import { RESERVATION_STATUS_OPTIONS } from "@/lib/reservations/labels";
import { updateReservationByAdmin } from "@/lib/reservations/staff-actions";
import {
  inputDateToDdMmYyyy,
  normalizeReservationTime,
  todayInputDateValue,
} from "@/lib/reservations/slots";

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

type Props = {
  row: ReservationModel | null;
  onClose: () => void;
  onSaved: () => void;
};

export function StaffReservationEditDialog({ row, onClose, onSaved }: Props) {
  const [tableNumber, setTableNumber] = useState<number | "">("");
  const [guests, setGuests] = useState(2);
  const [reservationDate, setReservationDate] = useState(todayInputDateValue);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [status, setStatus] = useState<ReservationStatus>("PENDING");
  const [special, setSpecial] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const { tables, loading: tablesLoading, error: tablesError } = useRestaurantTables({
    enabled: !!row,
    freshSnapshot: true,
  });

  const tablesForSelect = useMemo(() => {
    if (!row || tableNumber === "") return tables;
    if (tables.some((t) => t.tableNumber === tableNumber)) return tables;
    return [
      ...tables,
      {
        id: -row.tableNumber,
        tableNumber: row.tableNumber,
        capacity: row.numberOfGuests,
        tableStatus: "RESERVED",
      },
    ];
  }, [row, tableNumber, tables]);

  const tableCapacity = useMemo(
    () => tableCapacityFor(tablesForSelect, tableNumber),
    [tablesForSelect, tableNumber],
  );

  const {
    times: selectableTimes,
    loading: slotsLoading,
    error: slotsError,
  } = useReservationSlots(tableNumber, reservationDate, {
    keepTime: normalizeReservationTime(row?.reservationTime),
  });

  useEffect(() => {
    if (!row) return;
    setTableNumber(row.tableNumber);
    setGuests(row.numberOfGuests);
    setReservationDate(birthDdMmYyyyToInputDate(row.reservationDate) || todayInputDateValue());
    setSelectedTime(normalizeReservationTime(row.reservationTime));
    setStatus((row.reservationStatus?.toUpperCase() ?? "PENDING") as ReservationStatus);
    setSpecial(row.specialRequest ?? "");
    setError(null);
  }, [row]);

  useEffect(() => {
    if (!row) return;
    const origDate = birthDdMmYyyyToInputDate(row.reservationDate);
    if (reservationDate === origDate) return;
    setSelectedTime(null);
  }, [row, reservationDate]);

  useEffect(() => {
    if (!row || slotsLoading) return;
    const origTime = normalizeReservationTime(row.reservationTime);
    const origDate = birthDdMmYyyyToInputDate(row.reservationDate);
    if (reservationDate !== origDate || !origTime) return;
    if (selectableTimes.length === 0) return;
    setSelectedTime((cur) => {
      if (cur && selectableTimes.includes(cur)) return cur;
      return selectableTimes.includes(origTime) ? origTime : cur;
    });
  }, [row, slotsLoading, selectableTimes, reservationDate]);

  useEffect(() => {
    if (!selectedTime || slotsLoading) return;
    if (selectableTimes.length === 0) return;
    if (!selectableTimes.includes(selectedTime)) setSelectedTime(null);
  }, [selectableTimes, selectedTime, slotsLoading]);

  useEffect(() => {
    if (tableCapacity == null) return;
    setGuests((g) => clampGuestsForTable(g, tableCapacity));
  }, [tableCapacity]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!row) return;
    if (tableNumber === "") {
      setError("Chọn bàn");
      return;
    }
    if (!selectedTime) {
      setError("Chọn khung giờ");
      return;
    }
    if (tableCapacity != null && guests > tableCapacity) {
      setError(`Bàn ${tableNumber} tối đa ${tableCapacity} khách`);
      return;
    }
    if (!row.customerEmail?.trim()) {
      setError("Thiếu email khách — không thể cập nhật");
      return;
    }

    const payload: ReservationAdminRequestModel = {
      customerName: row.customerName ?? "",
      customerPhone: row.customerPhone ?? "",
      customerEmail: row.customerEmail,
      tableNumber,
      numberOfGuests: guests,
      reservationDate: inputDateToDdMmYyyy(reservationDate),
      reservationTime: selectedTime,
      reservationStatus: status,
      specialRequest: special.trim() || undefined,
    };

    setError(null);
    setPending(true);
    try {
      await updateReservationByAdmin(row.id, payload);
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Cập nhật thất bại");
    } finally {
      setPending(false);
    }
  }

  if (!row) return null;

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="max-h-[92vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-stone-200 bg-surface p-6 shadow-xl"
        role="dialog"
        aria-labelledby="staff-reservation-edit-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2
          id="staff-reservation-edit-title"
          className="font-serif text-xl font-semibold text-brand-900"
          style={{ fontFamily: "var(--font-cormorant), serif" }}
        >
          Sửa đặt bàn <span className="text-base font-mono">#{row.id}</span>
        </h2>
        <p className="mt-1 text-xs text-muted">
          Khách: {row.customerName ?? "—"} · {row.customerEmail ?? "—"}
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          {error ? (
            <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>
          ) : null}

          <label className="block text-xs font-medium text-stone-600">
            Trạng thái
            <select
              className={fieldClass}
              value={status}
              onChange={(e) => setStatus(e.target.value as ReservationStatus)}
            >
              {RESERVATION_STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>

          <TableNumberSelect
            id="staff-edit-reservation-table"
            value={tableNumber}
            onChange={setTableNumber}
            tables={tablesForSelect}
            loading={tablesLoading}
            error={tablesError}
            emptyHint="Không tải được danh sách bàn."
          />

          <div className="grid gap-4 sm:grid-cols-2">
            <ReservationGuestsInput
              id="staff-edit-reservation-guests"
              value={guests}
              onChange={setGuests}
              capacity={tableCapacity}
              disabled={tableNumber === ""}
            />
            <div>
              <label htmlFor="staff-edit-reservation-date" className="block text-sm font-medium text-stone-700">
                Ngày
              </label>
              <input
                id="staff-edit-reservation-date"
                type="date"
                value={reservationDate}
                min={todayInputDateValue()}
                onChange={(e) => setReservationDate(e.target.value)}
                className={fieldClass}
                required
              />
            </div>
          </div>

          <ReservationTimeSlotPicker
            id="staff-edit-reservation-time"
            times={selectableTimes}
            loading={slotsLoading}
            error={slotsError}
            selectedTime={selectedTime}
            onSelectTime={setSelectedTime}
            disabled={tableNumber === ""}
          />

          <div>
            <label htmlFor="staff-edit-reservation-special" className="block text-sm font-medium text-stone-700">
              Yêu cầu đặc biệt
            </label>
            <textarea
              id="staff-edit-reservation-special"
              value={special}
              onChange={(e) => setSpecial(e.target.value)}
              maxLength={300}
              rows={2}
              className={fieldClass}
            />
          </div>

          <div className="flex flex-wrap gap-3 pt-2">
            <button
              type="submit"
              disabled={
                pending ||
                tableNumber === "" ||
                !selectedTime ||
                (tableCapacity != null && guests > tableCapacity)
              }
              className="rounded-xl bg-brand-800 px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50"
            >
              {pending ? "Đang lưu…" : "Lưu"}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="rounded-xl border border-stone-300 px-5 py-2.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
            >
              Hủy
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

