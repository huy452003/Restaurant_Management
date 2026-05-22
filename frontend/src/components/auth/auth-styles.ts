/** Input kiểu form auth (nền xám nhạt, bo tròn) */
export const authFieldShell =
  "w-full rounded-xl border-0 bg-stone-100 text-stone-900 shadow-sm transition outline-none hover:bg-stone-100/90 focus:bg-white focus:ring-2 focus:ring-brand-600/20 autofill:shadow-[inset_0_0_0px_1000px_#f5f5f4] autofill:[-webkit-text-fill-color:#1c1917]";

/** Chiều cao đồng nhất giữa các ô trong lưới đăng ký */
export const authControlMinH = "min-h-[3.25rem]";

export const authIconSlot =
  "pointer-events-none absolute left-3.5 top-[1.35rem] z-10 -translate-y-1/2 text-stone-400";

export const authPrimaryButton =
  "w-full rounded-full bg-brand-800 py-3.5 text-sm font-bold uppercase tracking-wide text-white shadow-lg shadow-brand-900/20 transition hover:bg-brand-900 hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-50";

export const authSecondaryButton =
  "rounded-full border-2 border-brand-200 px-4 py-2.5 text-sm font-medium text-brand-800 transition hover:bg-brand-50 disabled:cursor-not-allowed disabled:opacity-50";

export const authSelectClass = `${authFieldShell} ${authControlMinH} px-4 py-0`;

export const authBirthSegmentClass = `${authFieldShell} ${authControlMinH} px-2 py-0 tabular-nums text-center`;

export const authGridRowClass = "grid grid-cols-1 gap-5 sm:grid-cols-2 sm:items-start sm:gap-4";

export const authReadonlyInputClass = `${authFieldShell} ${authControlMinH} cursor-not-allowed px-4 py-0 text-stone-500 opacity-80`;
