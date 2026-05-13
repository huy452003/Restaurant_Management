"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { StaffBackLink } from "@/components/staff/StaffBackLink";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { PaginatedResponse, ReservationModel } from "@/lib/api/types";

export default function StaffReservationsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [rows, setRows] = useState<ReservationModel[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<ReservationModel>>(
        `/reservations/filters/admin?${buildPageParams(0, 40)}`,
      );
      setRows(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được đặt chỗ");
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/staff/reservations");
      return;
    }
    if (!hasRole("ADMIN", "MANAGER")) {
      router.replace("/staff");
      return;
    }
    void Promise.resolve().then(() => load());
  }, [user, authLoading, hasRole, router, load]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <StaffBackLink />
      <h1 className="font-serif text-2xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Đặt chỗ
      </h1>
      {error ? <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">{error}</p> : null}
      {loading ? (
        <p className="mt-8 text-muted">Đang tải…</p>
      ) : (
        <div className="mt-6 overflow-x-auto rounded-xl border border-stone-200">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-stone-100 text-stone-600">
              <tr>
                <th className="px-3 py-3 font-medium">Khách</th>
                <th className="px-3 py-3 font-medium">SĐT</th>
                <th className="px-3 py-3 font-medium">Bàn</th>
                <th className="px-3 py-3 font-medium">Giờ</th>
                <th className="px-3 py-3 font-medium">Số khách</th>
                <th className="px-3 py-3 font-medium">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id} className="border-t border-stone-100 bg-surface hover:bg-stone-50/80">
                  <td className="px-3 py-2.5">{r.customerName ?? "—"}</td>
                  <td className="px-3 py-2.5 text-muted">{r.customerPhone ?? "—"}</td>
                  <td className="px-3 py-2.5">{r.tableNumber}</td>
                  <td className="px-3 py-2.5 text-xs">{r.reservationTs}</td>
                  <td className="px-3 py-2.5">{r.numberOfGuests}</td>
                  <td className="px-3 py-2.5">{r.reservationStatus}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
