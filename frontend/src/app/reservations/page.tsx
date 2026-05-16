"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { TableNumberSelect } from "@/components/TableNumberSelect";
import { useAuth } from "@/context/auth-context";
import { useRestaurantTables } from "@/hooks/use-restaurant-tables";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { PaginatedResponse, ReservationModel } from "@/lib/api/types";
import { formatLocalDateTimeDdMmYyyyHhMmSs } from "@/lib/dates";

export default function ReservationsPage() {
  const { user, loading: authLoading, hasRole } = useAuth();
  const router = useRouter();
  const [list, setList] = useState<ReservationModel[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [formError, setFormError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [tableNumber, setTableNumber] = useState<number | "">("");
  const { tables, loading: tablesLoading, error: tablesError } = useRestaurantTables({
    tableStatus: "AVAILABLE",
    excludeTablesWithPendingOrder: true,
    enabled: !!user && hasRole("CUSTOMER"),
  });
  const [guests, setGuests] = useState(2);
  const [whenLocal, setWhenLocal] = useState(""); // datetime-local
  const [special, setSpecial] = useState("");

  const load = useCallback(async () => {
    if (!user || !hasRole("CUSTOMER")) return;
    setLoading(true);
    setError(null);
    try {
      const res = await apiFetch<PaginatedResponse<ReservationModel>>(
        `/reservations/filters?${buildPageParams(0, 20)}`,
      );
      setList(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được đặt bàn");
    } finally {
      setLoading(false);
    }
  }, [user, hasRole]);

  useEffect(() => {
    if (authLoading) return;
    if (!user) {
      router.replace("/login?next=/reservations");
      return;
    }
    if (!hasRole("CUSTOMER")) {
      void Promise.resolve().then(() => setLoading(false));
      return;
    }
    void Promise.resolve().then(() => load());
  }, [user, authLoading, hasRole, router, load]);

  useEffect(() => {
    if (tables.length === 0) return;
    if (tableNumber === "" || !tables.some((t) => t.tableNumber === tableNumber)) {
      setTableNumber(tables[0].tableNumber);
    }
  }, [tables, tableNumber]);

  async function createReservation(e: React.FormEvent) {
    e.preventDefault();
    if (tableNumber === "") {
      setFormError("Chọn bàn");
      return;
    }
    if (!whenLocal) {
      setFormError("Chọn ngày giờ");
      return;
    }
    const ts = formatLocalDateTimeDdMmYyyyHhMmSs(new Date(whenLocal));
    if (!ts) {
      setFormError("Thời gian không hợp lệ");
      return;
    }
    setFormError(null);
    setPending(true);
    try {
      await apiFetch<ReservationModel[]>("/reservations", {
        method: "POST",
        body: JSON.stringify([
          {
            tableNumber,
            numberOfGuests: guests,
            reservationTs: ts,
            specialRequest: special.trim() || undefined,
          },
        ]),
      });
      setSpecial("");
      await load();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "Tạo đặt bàn thất bại");
    } finally {
      setPending(false);
    }
  }

  async function cancelReservation(id: number) {
    setError(null);
    try {
      await apiFetch<ReservationModel>(`/reservations/cancel/${id}`, { method: "PATCH" });
      await load();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Hủy thất bại");
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
      <h1
        className="font-serif text-3xl font-semibold text-brand-900"
        style={{ fontFamily: "var(--font-cormorant), serif" }}
      >
        Đặt bàn
      </h1>

      <form
        onSubmit={createReservation}
        className="mt-8 grid gap-4 rounded-2xl border border-stone-200 bg-surface p-6 shadow-sm sm:grid-cols-2"
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
          <label className="block text-sm font-medium text-stone-700">Số khách</label>
          <input
            type="number"
            min={1}
            value={guests}
            onChange={(e) => setGuests(Number(e.target.value) || 1)}
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm"
          />
        </div>
        <div className="sm:col-span-2">
          <label className="block text-sm font-medium text-stone-700">Thời gian</label>
          <input
            type="datetime-local"
            value={whenLocal}
            onChange={(e) => setWhenLocal(e.target.value)}
            className="mt-1 w-full max-w-md rounded-lg border border-stone-300 px-3 py-2 text-sm"
            required
          />
        </div>
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
            disabled={pending || tableNumber === "" || tables.length === 0}
            className="rounded-xl bg-brand-800 px-6 py-3 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50"
          >
            {pending ? "Đang gửi…" : "Gửi yêu cầu đặt bàn"}
          </button>
        </div>
      </form>

      {error ? <p className="mt-4 text-sm text-red-700">{error}</p> : null}

      <h2 className="mt-12 font-serif text-xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
        Lịch của bạn
      </h2>
      {loading ? (
        <p className="mt-4 text-muted">Đang tải…</p>
      ) : list.length === 0 ? (
        <p className="mt-4 text-sm text-muted">Chưa có lịch đặt.</p>
      ) : (
        <ul className="mt-4 space-y-3">
          {list.map((r) => (
            <li
              key={r.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-stone-200 bg-surface px-4 py-3 text-sm"
            >
              <div>
                <p className="font-medium text-stone-900">
                  Bàn {r.tableNumber} · {r.numberOfGuests} khách
                </p>
                <p className="text-muted">{r.reservationTs}</p>
                <p className="text-xs text-stone-500">{r.reservationStatus}</p>
              </div>
              <button
                type="button"
                onClick={() => void cancelReservation(r.id)}
                className="rounded-lg border border-stone-300 px-3 py-1.5 text-xs font-medium hover:bg-stone-50"
              >
                Hủy
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
