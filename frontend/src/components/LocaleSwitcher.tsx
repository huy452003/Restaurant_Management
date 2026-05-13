"use client";

import { useAppLocale } from "@/context/locale-context";
import type { AppLocale } from "@/lib/locale";

export function LocaleSwitcher() {
  const { locale, setLocale } = useAppLocale();

  function select(next: AppLocale) {
    if (next !== locale) setLocale(next);
  }

  return (
    <div
      className="flex shrink-0 items-center rounded-lg border border-stone-200 bg-stone-50/90 p-0.5 shadow-sm"
      role="group"
      aria-label="Ngôn ngữ API"
    >
      <button
        type="button"
        onClick={() => select("vi")}
        className={`rounded-md px-2.5 py-1.5 text-xs font-bold tabular-nums transition ${
          locale === "vi"
            ? "bg-brand-800 text-white shadow-sm"
            : "text-stone-600 hover:bg-stone-200/80 hover:text-stone-900"
        }`}
      >
        VI
      </button>
      <button
        type="button"
        onClick={() => select("en")}
        className={`rounded-md px-2.5 py-1.5 text-xs font-bold tabular-nums transition ${
          locale === "en"
            ? "bg-brand-800 text-white shadow-sm"
            : "text-stone-600 hover:bg-stone-200/80 hover:text-stone-900"
        }`}
      >
        EN
      </button>
    </div>
  );
}
