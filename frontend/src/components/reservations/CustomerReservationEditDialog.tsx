"use client";

import { useEffect, useMemo, useState } from "react";
import { ReservationGuestsInput } from "@/components/reservations/ReservationGuestsInput";
import { ReservationTimeSlotPicker } from "@/components/reservations/ReservationTimeSlotPicker";
import { useReservationSlots } from "@/hooks/use-reservation-slots";
import { useRestaurantTables } from "@/hooks/use-restaurant-tables";
import { apiFetch, ApiError } from "@/lib/api/client";
import type {
  ReservationCustomerUpdateModel,
  ReservationModel,
} from "@/lib/api/types";
import { birthDdMmYyyyToInputDate } from "@/lib/dates";
import { clampGuestsForTable, tableCapacityFor } from "@/lib/reservations/guests";
import {
  inputDateToDdMmYyyy,
  normalizeReservationTime,
  todayInputDateValue,
} from "@/lib/reservations/slots";

type Props = {
  open: boolean;
  reservation: ReservationModel | null;
  onClose: () => void;
  onSaved: () => void;
};

export function CustomerReservationEditDialog({ open, reservation, onClose, onSaved }: Props) {
  const [guests, setGuests] = useState(2);
  const [reservationDate, setReservationDate] = useState(todayInputDateValue);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [special, setSpecial] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const tableNumber = reservation?.tableNumber ?? "";

  const { tables } = useRestaurantTables({
    enabled: open && !!reservation,
  });

  const tableCapacity = useMemo(
    () => tableCapacityFor(tables, tableNumber),
    [tables, tableNumber],
  );

  const {
    times: selectableTimes,
    loading: slotsLoading,
    error: slotsError,
  } = useReservationSlots(tableNumber, reservationDate, {
    keepTime: normalizeReservationTime(reservation?.reservationTime),
  });

  useEffect(() => {
    if (!open || !reservation) return;
    setGuests(reservation.numberOfGuests);
    setReservationDate(birthDdMmYyyyToInputDate(reservation.reservationDate) || todayInputDateValue());
    setSelectedTime(normalizeReservationTime(reservation.reservationTime));
    setSpecial(reservation.specialRequest ?? "");
    setError(null);
  }, [open, reservation]);

  useEffect(() => {
    if (!open || !reservation) return;
    const origDate = birthDdMmYyyyToInputDate(reservation.reservationDate);
    if (reservationDate === origDate) return;
    setSelectedTime(null);
  }, [open, reservation, reservationDate]);

  useEffect(() => {
    if (!open || !reservation || slotsLoading) return;
    const origTime = normalizeReservationTime(reservation.reservationTime);
    const origDate = birthDdMmYyyyToInputDate(reservation.reservationDate);
    if (reservationDate !== origDate || !origTime) return;
    if (selectableTimes.length === 0) return;
    setSelectedTime((cur) => {
      if (cur && selectableTimes.includes(cur)) return cur;
      return selectableTimes.includes(origTime) ? origTime : cur;
    });
  }, [open, reservation, slotsLoading, selectableTimes, reservationDate]);

  useEffect(() => {
    if (!selectedTime || slotsLoading) return;
    if (selectableTimes.length === 0) return;
    if (!selectableTimes.includes(selectedTime)) setSelectedTime(null);
  }, [selectableTimes, selectedTime, slotsLoading]);

  useEffect(() => {
    if (tableCapacity == null) return;
    setGuests((g) => clampGuestsForTable(g, tableCapacity));
  }, [tableCapacity]);

  async function save() {
    if (!reservation) return;
    if (!reservationDate.trim()) {
      setError("Chọn ngày");
      return;
    }
    if (!selectedTime) {
      setError("Chọn khung giờ");
      return;
    }
    if (tableCapacity != null && guests > tableCapacity) {
      setError(`Bàn ${reservation.tableNumber} tối đa ${tableCapacity} khách`);
      return;
    }
    setError(null);
    setSaving(true);
    try {
      const body: ReservationCustomerUpdateModel = {
        numberOfGuests: guests,
        reservationDate: inputDateToDdMmYyyy(reservationDate),
        reservationTime: selectedTime,
        specialRequest: special.trim() || undefined,
      };
      await apiFetch<ReservationModel>(`/reservations/${reservation.id}`, {
        method: "PATCH",
        body: JSON.stringify(body),
      });
      onSaved();
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Cập nhật thất bại");
    } finally {
      setSaving(false);
    }
  }

  if (!open || !reservation) return null;

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
        aria-labelledby="reservation-edit-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="reservation-edit-title"
              className="font-serif text-xl font-semibold text-brand-900"
              style={{ fontFamily: "var(--font-cormorant), serif" }}
            >
              Sửa đặt bàn
            </h2>
            <p className="mt-1 text-sm text-muted">
              Mã #{reservation.id}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="shrink-0 rounded-lg border border-stone-200 px-3 py-1.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
          >
            Đóng
          </button>
        </div>

        {error ? (
          <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>
        ) : null}

        <form
          className="mt-6 space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            void save();
          }}
        >
          <div className="rounded-lg border border-stone-200 bg-stone-50/80 px-3 py-2.5 text-sm">
            <p className="font-medium text-stone-800">Bàn {reservation.tableNumber}</p>
            <p className="mt-0.5 text-xs text-muted">
              {tableCapacity != null
                ? `Sức chứa ${tableCapacity} khách · không đổi bàn sau khi đặt`
                : "Không đổi bàn sau khi đặt — liên hệ nhà hàng nếu cần hỗ trợ"}
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <ReservationGuestsInput
              id="edit-reservation-guests"
              value={guests}
              onChange={setGuests}
              capacity={tableCapacity}
            />
            <div>
              <label htmlFor="edit-reservation-date" className="block text-sm font-medium text-stone-700">
                Ngày
              </label>
              <input
                id="edit-reservation-date"
                type="date"
                value={reservationDate}
                min={todayInputDateValue()}
                onChange={(e) => setReservationDate(e.target.value)}
                className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
                required
              />
            </div>
          </div>

          <ReservationTimeSlotPicker
            id="edit-reservation-time"
            times={selectableTimes}
            loading={slotsLoading}
            error={slotsError}
            selectedTime={selectedTime}
            onSelectTime={setSelectedTime}
            disabled={tableNumber === ""}
          />

          <div>
            <label htmlFor="edit-reservation-special" className="block text-sm font-medium text-stone-700">
              Yêu cầu đặc biệt
            </label>
            <textarea
              id="edit-reservation-special"
              value={special}
              onChange={(e) => setSpecial(e.target.value)}
              maxLength={300}
              rows={2}
              className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-600/25"
              placeholder="Ghế trẻ em, sinh nhật…"
            />
          </div>

          <div className="flex flex-wrap gap-3 pt-2">
            <button
              type="submit"
              disabled={
                saving ||
                !selectedTime ||
                (tableCapacity != null && guests > tableCapacity)
              }
              className="rounded-xl bg-brand-800 px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50"
            >
              {saving ? "Đang lưu…" : "Lưu thay đổi"}
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
