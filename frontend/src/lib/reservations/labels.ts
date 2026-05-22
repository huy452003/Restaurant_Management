export const RESERVATION_STATUS_LABEL_VI: Record<string, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  COMPLETED: "Hoàn tất",
  NO_SHOW: "Không đến",
  CANCELLED: "Đã hủy",
};

export function reservationStatusLabel(status: string | undefined): string {
  if (!status) return "—";
  return RESERVATION_STATUS_LABEL_VI[status.toUpperCase()] ?? status;
}

export function isReservationEditable(status: string | undefined): boolean {
  return (status?.toUpperCase() ?? "") === "PENDING";
}

/** Hủy qua PATCH /reservations/cancel — PENDING hoặc CONFIRMED. */
export function isReservationCancellable(status: string | undefined): boolean {
  const s = status?.toUpperCase() ?? "";
  return s === "PENDING" || s === "CONFIRMED";
}

/** Staff (admin/manager/cashier) sửa qua PUT /reservations/admin. */
export function isReservationAdminEditable(status: string | undefined): boolean {
  const s = status?.toUpperCase() ?? "";
  return s !== "CANCELLED" && s !== "COMPLETED" && s !== "NO_SHOW";
}

export const RESERVATION_STATUS_OPTIONS = Object.entries(RESERVATION_STATUS_LABEL_VI).map(
  ([value, label]) => ({ value, label }),
);
