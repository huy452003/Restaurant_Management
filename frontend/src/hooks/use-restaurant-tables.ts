"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { PaginatedResponse, TableModel } from "@/lib/api/types";

type Options = {
  /** Ví dụ `AVAILABLE` khi đặt bàn. */
  tableStatus?: string;
  /** Ẩn bàn đang có đơn PENDING (giỏ hàng / đặt bàn khách). */
  excludeTablesWithPendingOrder?: boolean;
  /** Bỏ cache Redis — dùng khi cần trạng thái bàn mới nhất (sửa đơn staff). */
  freshSnapshot?: boolean;
  enabled?: boolean;
};

export function useRestaurantTables(options: Options = {}) {
  const { tableStatus, excludeTablesWithPendingOrder = false, freshSnapshot = false, enabled = true } =
    options;
  const [tables, setTables] = useState<TableModel[]>([]);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!enabled) return;
    setLoading(true);
    setError(null);
    try {
      const extra: Record<string, string | number | boolean | undefined> = {};
      if (tableStatus) extra.tableStatus = tableStatus;
      if (excludeTablesWithPendingOrder) extra.excludeTablesWithPendingOrder = true;
      const qs = buildPageParams(0, 200, extra);
      const res = await apiFetch<PaginatedResponse<TableModel>>(`/tables/filters?${qs}`);
      setTables(res.data.content ?? []);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Không tải được danh sách bàn");
      setTables([]);
    } finally {
      setLoading(false);
    }
  }, [enabled, tableStatus, excludeTablesWithPendingOrder, freshSnapshot]);

  useEffect(() => {
    void load();
  }, [load]);

  const tablesSorted = useMemo(
    () => [...tables].sort((a, b) => a.tableNumber - b.tableNumber),
    [tables],
  );

  return { tables: tablesSorted, loading, error, reload: load };
}
