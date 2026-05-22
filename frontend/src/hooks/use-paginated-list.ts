"use client";

import { useCallback, useRef, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { PaginatedResponse } from "@/lib/api/types";
import type { FilterValues } from "@/lib/list/utils";
import { buildFilterUrl } from "@/lib/list/utils";

type Config = {
  basePath: string;
  pageSize?: number;
  initialFilters?: FilterValues;
  extraParams?: Record<string, string | number | boolean>;
};

const EMPTY_FILTERS: FilterValues = {};

export function usePaginatedList<T>(config: Config) {
  const basePath = config.basePath;
  const pageSize = config.pageSize ?? 15;
  const initialFiltersRef = useRef(config.initialFilters ?? EMPTY_FILTERS);
  initialFiltersRef.current = config.initialFilters ?? EMPTY_FILTERS;
  const extraRef = useRef(config.extraParams);
  extraRef.current = config.extraParams;

  const [rows, setRows] = useState<T[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [draftFilters, setDraftFilters] = useState<FilterValues>(
    () => config.initialFilters ?? EMPTY_FILTERS,
  );
  const [appliedFilters, setAppliedFilters] = useState<FilterValues>(
    () => config.initialFilters ?? EMPTY_FILTERS,
  );
  const appliedRef = useRef(appliedFilters);
  appliedRef.current = appliedFilters;

  const load = useCallback(
    async (pageIndex: number, filters: FilterValues) => {
      setLoading(true);
      setError(null);
      try {
        const url = buildFilterUrl(basePath, pageIndex, pageSize, filters, extraRef.current);
        const res = await apiFetch<PaginatedResponse<T>>(url);
        setRows(res.data.content ?? []);
        const totalElements = Number(res.data.totalElements ?? 0);
        const responseSize = res.data.size ?? pageSize;
        let totalPages = res.data.totalPages ?? 0;
        if (totalPages <= 0 && totalElements > 0) {
          totalPages = Math.max(1, Math.ceil(totalElements / responseSize));
        }
        setTotalPages(totalPages);
        setTotalElements(totalElements);
        setPage(res.data.page ?? pageIndex);
        setAppliedFilters(filters);
        appliedRef.current = filters;
      } catch (e) {
        setError(e instanceof ApiError ? e.message : "Không tải được dữ liệu");
        setRows([]);
        setTotalPages(0);
        setTotalElements(0);
      } finally {
        setLoading(false);
      }
    },
    [basePath, pageSize],
  );

  const reload = useCallback(() => {
    void load(page, appliedRef.current);
  }, [load, page]);

  const goToPage = useCallback(
    (nextPage: number) => {
      void load(nextPage, appliedRef.current);
    },
    [load],
  );

  const applyFilters = useCallback(() => {
    void load(0, draftFilters);
  }, [draftFilters, load]);

  const resetFilters = useCallback(() => {
    const initial = initialFiltersRef.current;
    setDraftFilters(initial);
    void load(0, initial);
  }, [load]);

  const setFilter = useCallback((key: string, value: string) => {
    setDraftFilters((prev) => ({ ...prev, [key]: value }));
  }, []);

  const setFilters = useCallback((next: FilterValues) => {
    setDraftFilters(next);
  }, []);

  const loadInitial = useCallback(() => {
    void load(0, initialFiltersRef.current);
  }, [load]);

  const search = useCallback(
    (filters: FilterValues, pageIndex = 0) => {
      setDraftFilters(filters);
      void load(pageIndex, filters);
    },
    [load],
  );

  return {
    rows,
    page,
    totalPages,
    totalElements,
    loading,
    error,
    draftFilters,
    appliedFilters,
    setFilter,
    setFilters,
    applyFilters,
    resetFilters,
    reload,
    goToPage,
    loadInitial,
    search,
    pageSize,
  };
}
