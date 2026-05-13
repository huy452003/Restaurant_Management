export const APP_LOCALE_CHANGE_EVENT = "app-locale-change";

export const APP_LOCALE_STORAGE_KEY = "restaurant-ui-locale";

export type AppLocale = "vi" | "en";

/** Mặc định EN — khớp message chi tiết tiếng Anh từ service; user chọn VI để backend trả bản dịch. */
export const DEFAULT_APP_LOCALE: AppLocale = "en";

export function isAppLocale(v: string | null | undefined): v is AppLocale {
  return v === "vi" || v === "en";
}

export function getStoredAppLocale(): AppLocale {
  if (typeof window === "undefined") return DEFAULT_APP_LOCALE;
  const raw = window.localStorage.getItem(APP_LOCALE_STORAGE_KEY);
  return isAppLocale(raw) ? raw : DEFAULT_APP_LOCALE;
}

export function setStoredAppLocale(locale: AppLocale): void {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(APP_LOCALE_STORAGE_KEY, locale);
  window.dispatchEvent(new CustomEvent(APP_LOCALE_CHANGE_EVENT, { detail: locale }));
}

/** Giá trị header gửi Spring LocaleResolver */
export function acceptLanguageHeader(locale: AppLocale): string {
  if (locale === "vi") return "vi,en;q=0.6";
  return "en,vi;q=0.6";
}
