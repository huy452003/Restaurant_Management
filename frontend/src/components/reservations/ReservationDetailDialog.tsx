"use client";

import { useEffect } from "react";
import { StatusBadge } from "@/components/ui/StatusBadge";
import type { ReservationModel } from "@/lib/api/types";
import { formatReservationSchedule } from "@/lib/reservations/slots";

type Props = {
  open: boolean;
  reservation: ReservationModel | null;
  onClose: () => void;
  onEdit?: () => void;
};

export function ReservationDetailDialog({ open, reservation, onClose, onEdit }: Props) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

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
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-stone-200 bg-surface p-6 shadow-xl"
        role="dialog"
        aria-labelledby="reservation-detail-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2
              id="reservation-detail-title"
              className="font-serif text-xl font-semibold text-brand-900"
              style={{ fontFamily: "var(--font-cormorant), serif" }}
            >
              Chi tiết đặt bàn
            </h2>
            <p className="mt-1 text-sm text-muted">Mã #{reservation.id}</p>
          </div>
          <div className="flex shrink-0 flex-wrap gap-2">
            {onEdit ? (
              <button
                type="button"
                onClick={onEdit}
                className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-sm font-semibold text-brand-900 hover:bg-brand-100"
              >
                Chỉnh sửa
              </button>
            ) : null}
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-stone-200 px-3 py-1.5 text-sm font-medium text-stone-700 hover:bg-stone-50"
            >
              Đóng
            </button>
          </div>
        </div>

        <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2">
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Trạng thái</dt>
            <dd className="mt-1">
              <StatusBadge domain="reservation" status={reservation.reservationStatus} />
            </dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Bàn</dt>
            <dd className="mt-1 font-medium text-stone-900">Bàn {reservation.tableNumber}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Lịch đặt</dt>
            <dd className="mt-1 font-medium text-stone-900">{formatReservationSchedule(reservation)}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Số khách</dt>
            <dd className="mt-1 font-medium text-stone-900">{reservation.numberOfGuests}</dd>
          </div>
          <div className="sm:col-span-2">
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Khách</dt>
            <dd className="mt-1 font-medium text-stone-900">{reservation.customerName ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">SĐT</dt>
            <dd className="mt-1 text-stone-800">{reservation.customerPhone ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Email</dt>
            <dd className="mt-1 break-all text-stone-800">{reservation.customerEmail ?? "—"}</dd>
          </div>
          {reservation.specialRequest?.trim() ? (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Yêu cầu</dt>
              <dd className="mt-1 text-stone-800">{reservation.specialRequest}</dd>
            </div>
          ) : null}
          {reservation.createdAt ? (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Tạo lúc</dt>
              <dd className="mt-1 text-stone-800">{reservation.createdAt}</dd>
            </div>
          ) : null}
          {reservation.updatedAt ? (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium uppercase tracking-wide text-stone-500">Cập nhật</dt>
              <dd className="mt-1 text-stone-800">{reservation.updatedAt}</dd>
            </div>
          ) : null}
        </dl>
      </div>
    </div>
  );
}
