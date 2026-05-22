import type { TableModel } from "@/lib/api/types";

export function tableCapacityFor(
  tables: TableModel[],
  tableNumber: number | "",
): number | null {
  if (tableNumber === "") return null;
  const t = tables.find((x) => x.tableNumber === tableNumber);
  return t?.capacity && t.capacity > 0 ? t.capacity : null;
}

/** Giữ số khách trong [1, sức chứa bàn]. */
export function clampGuestsForTable(guests: number, capacity: number | null): number {
  const min = 1;
  if (capacity == null || capacity < 1) return Math.max(min, guests);
  return Math.min(Math.max(min, guests), capacity);
}
