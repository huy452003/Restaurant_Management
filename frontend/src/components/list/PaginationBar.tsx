"use client";

import { getVisiblePageIndices } from "@/lib/list/pagination-window";

const VISIBLE_PAGE_COUNT = 5;

type Props = {
  page: number;
  totalPages: number;
  totalElements: number;
  loading?: boolean;
  onPageChange: (page: number) => void;
  unitLabel?: string;
};

const navBtnClass =
  "rounded-lg border border-stone-300 px-3 py-2 text-sm font-medium text-stone-700 hover:bg-stone-50 disabled:cursor-not-allowed disabled:opacity-40";

const pageBtnClass =
  "min-w-[2.25rem] rounded-lg border px-3 py-2 text-sm font-medium tabular-nums transition";

export function PaginationBar({
  page,
  totalPages,
  totalElements,
  loading,
  onPageChange,
  unitLabel = "bản ghi",
}: Props) {
  if (totalPages <= 0 && totalElements === 0) return null;

  const visiblePages = getVisiblePageIndices(page, totalPages, VISIBLE_PAGE_COUNT);
  const showPager = totalPages > 1;
  const canGoPrev = page > 0;
  const canGoNext = page < totalPages - 1;
  const windowStartsAfterFirst = visiblePages[0] > 0;
  const windowEndsBeforeLast = visiblePages[visiblePages.length - 1] < totalPages - 1;

  return (
    <div className="mt-6 flex flex-col items-center gap-3">
      <p className="text-center text-sm text-muted">
        Tổng <span className="font-medium text-stone-800">{totalElements}</span> {unitLabel}
        {showPager ? (
          <>
            {" "}
            · Trang {page + 1}/{totalPages}
          </>
        ) : null}
      </p>

      {showPager ? (
        <nav className="flex flex-wrap items-center justify-center gap-1.5" aria-label="Phân trang">
          <button
            type="button"
            disabled={!canGoPrev || loading}
            onClick={() => onPageChange(0)}
            className={navBtnClass}
            title="Trang đầu"
          >
            «
          </button>
          <button
            type="button"
            disabled={!canGoPrev || loading}
            onClick={() => onPageChange(page - 1)}
            className={navBtnClass}
          >
            Trước
          </button>

          {windowStartsAfterFirst ? (
            <span className="px-1 text-sm text-stone-400" aria-hidden>
              …
            </span>
          ) : null}

          {visiblePages.map((p) => {
            const active = p === page;
            return (
              <button
                key={p}
                type="button"
                disabled={loading}
                onClick={() => onPageChange(p)}
                aria-current={active ? "page" : undefined}
                className={`${pageBtnClass} ${
                  active
                    ? "border-brand-700 bg-brand-800 text-white shadow-sm"
                    : "border-stone-300 text-stone-700 hover:bg-stone-50"
                }`}
              >
                {p + 1}
              </button>
            );
          })}

          {windowEndsBeforeLast ? (
            <span className="px-1 text-sm text-stone-400" aria-hidden>
              …
            </span>
          ) : null}

          <button
            type="button"
            disabled={!canGoNext || loading}
            onClick={() => onPageChange(page + 1)}
            className={navBtnClass}
          >
            Sau
          </button>
          <button
            type="button"
            disabled={!canGoNext || loading}
            onClick={() => onPageChange(totalPages - 1)}
            className={navBtnClass}
            title="Trang cuối"
          >
            »
          </button>
        </nav>
      ) : null}
    </div>
  );
}
