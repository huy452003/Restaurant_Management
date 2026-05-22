"use client";

import { useCallback, useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { ReservationAvailabilityModel } from "@/lib/api/types";
import { buildSelectableSlotTimes, inputDateToDdMmYyyy } from "@/lib/reservations/slots";

type Options = {
  /** Giữ khung giờ hiện tại khi sửa (không cần đổi API availability). */
  keepTime?: string | null;
};

export function useReservationSlots(
  tableNumber: number | "",
  dateYmd: string,
  options: Options = {},
) {
  const { keepTime = null } = options;
  const [times, setTimes] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (tableNumber === "" || !dateYmd.trim()) {
      setTimes([]);
      setError(null);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const qs = new URLSearchParams({
        tableNumber: String(tableNumber),
        date: inputDateToDdMmYyyy(dateYmd.trim()),
      });
      const res = await apiFetch<ReservationAvailabilityModel>(`/reservations/availability?${qs}`);
      setTimes(buildSelectableSlotTimes(res.data.bookedTimes ?? [], dateYmd.trim(), keepTime));
    } catch (e) {
      setTimes([]);
      setError(e instanceof ApiError ? e.message : "Không tải được khung giờ");
    } finally {
      setLoading(false);
    }
  }, [tableNumber, dateYmd, keepTime]);

  useEffect(() => {
    void load();
  }, [load]);

  return { times, loading, error, reload: load };
}
