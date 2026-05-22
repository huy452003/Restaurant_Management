import type { ReservationModel } from "@/lib/api/types";
import { formatBirthDdMmYyyy } from "@/lib/dates";

/** Khung cố định — mirror {@code ReservationSlot} backend (10:00–21:30, 30 phút). */
export const RESERVATION_SLOT_TIMES: readonly string[] = [
  "10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
  "14:00", "14:30", "15:00", "15:30", "16:00", "16:30", "17:00", "17:30",
  "18:00", "18:30", "19:00", "19:30", "20:00", "20:30", "21:00", "21:30",
] as const;

/** Chuẩn HH:mm (API có thể trả "19:00" hoặc "19:00:00"). */
export function normalizeReservationTime(time: string | null | undefined): string | null {
  if (!time?.trim()) return null;
  const match = time.trim().match(/^(\d{1,2}):(\d{2})/);
  if (!match) return null;
  return `${match[1].padStart(2, "0")}:${match[2]}`;
}

/** Dropdown: chỉ các HH:mm còn chọn được (không booked, không quá khứ nếu là hôm nay). */
export function buildSelectableSlotTimes(
  bookedTimes: string[],
  dateYmd: string,
  /** Khi sửa đặt bàn: luôn giữ khung đang giữ (API bookedTimes vẫn có slot của chính mình). */
  keepTime?: string | null,
): string[] {
  const kept = normalizeReservationTime(keepTime);
  const booked = new Set(bookedTimes.map((t) => normalizeReservationTime(t) ?? t));
  const times = RESERVATION_SLOT_TIMES.filter((time) => {
    if (booked.has(time)) return false;
    if (dateYmd !== todayInputDateValue()) return true;
    const [hh, mm] = time.split(":").map(Number);
    const [y, mo, d] = dateYmd.split("-").map(Number);
    return new Date(y, mo - 1, d, hh, mm, 0, 0).getTime() >= Date.now();
  });
  if (kept && !times.includes(kept)) {
    times.push(kept);
    times.sort();
  }
  return times;
}

/** yyyy-MM-dd (input type=date) → dd-MM-yyyy (API Jackson). */
export function inputDateToDdMmYyyy(ymd: string): string {
  if (!ymd.trim()) return "";
  return formatBirthDdMmYyyy(ymd.trim());
}

/** yyyy-MM-dd cho input type=date (hôm nay theo giờ máy khách). */
export function todayInputDateValue(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

export function formatReservationSchedule(r: ReservationModel): string {
  if (!r.reservationDate || !r.reservationTime) return "—";
  return `${r.reservationDate} · ${r.reservationTime}`;
}
