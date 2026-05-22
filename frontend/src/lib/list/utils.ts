import { buildPageParams } from "@/lib/api/client";

export type FilterValues = Record<string, string>;

export function filtersToQueryExtra(filters: FilterValues): Record<string, string | number | boolean> {
  const extra: Record<string, string | number | boolean> = {};
  for (const [key, value] of Object.entries(filters)) {
    const trimmed = value.trim();
    if (trimmed !== "") extra[key] = trimmed;
  }
  return extra;
}

export function buildFilterUrl(
  basePath: string,
  page: number,
  pageSize: number,
  filters: FilterValues,
  extra?: Record<string, string | number | boolean | undefined | null>,
): string {
  const qs = buildPageParams(page, pageSize, { ...filtersToQueryExtra(filters), ...extra });
  return `${basePath}?${qs}`;
}
