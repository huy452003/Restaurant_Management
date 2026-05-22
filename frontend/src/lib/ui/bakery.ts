/** Class dùng chung — phong bakery (nền kem, nâu chocolate, bo tròn mềm). */

export const fontSerif = { fontFamily: "var(--font-cormorant), serif" } as const;

export const pageTitleClass =
  "font-serif text-3xl font-semibold tracking-tight text-brand-900 sm:text-4xl";

export const pageSubtitleClass = "mt-2 max-w-xl text-sm leading-relaxed text-muted";

export const btnPrimaryClass =
  "inline-flex items-center justify-center rounded-full bg-brand-800 px-7 py-3 text-sm font-semibold text-white shadow-md shadow-brand-900/15 transition hover:bg-brand-900 hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-50";

export const btnSecondaryClass =
  "inline-flex items-center justify-center rounded-full border-2 border-brand-200 bg-white px-7 py-3 text-sm font-semibold text-brand-800 transition hover:border-brand-300 hover:bg-brand-50";

export const cardClass =
  "rounded-3xl border border-brand-100/80 bg-surface shadow-[0_12px_40px_-12px_rgba(74,55,40,0.12)]";

export const cardSoftClass =
  "rounded-3xl border border-brand-100 bg-brand-50/50 p-6 shadow-sm";

export const sectionLabelClass =
  "inline-flex rounded-full bg-blush px-4 py-1 text-xs font-semibold uppercase tracking-wider text-brand-800";
