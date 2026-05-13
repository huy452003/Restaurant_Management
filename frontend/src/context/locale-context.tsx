"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  APP_LOCALE_CHANGE_EVENT,
  type AppLocale,
  DEFAULT_APP_LOCALE,
  getStoredAppLocale,
  isAppLocale,
  setStoredAppLocale,
} from "@/lib/locale";

type LocaleContextValue = {
  locale: AppLocale;
  setLocale: (next: AppLocale) => void;
};

const LocaleContext = createContext<LocaleContextValue | null>(null);

export function LocaleProvider({ children }: { children: React.ReactNode }) {
  /** Luôn giống SSR lần đầu — tránh hydration mismatch (không đọc localStorage trong initializer). */
  const [locale, setLocaleState] = useState<AppLocale>(DEFAULT_APP_LOCALE);

  useEffect(() => {
    void Promise.resolve().then(() => setLocaleState(getStoredAppLocale()));
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale === "vi" ? "vi" : "en";
  }, [locale]);

  useEffect(() => {
    function onChange(ev: Event) {
      const ce = ev as CustomEvent<string>;
      if (isAppLocale(ce.detail)) setLocaleState(ce.detail);
      else setLocaleState(getStoredAppLocale());
    }
    window.addEventListener(APP_LOCALE_CHANGE_EVENT, onChange as EventListener);
    return () => window.removeEventListener(APP_LOCALE_CHANGE_EVENT, onChange as EventListener);
  }, []);

  const setLocale = useCallback((next: AppLocale) => {
    setStoredAppLocale(next);
    setLocaleState(next);
  }, []);

  const value = useMemo(() => ({ locale, setLocale }), [locale, setLocale]);

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useAppLocale() {
  const ctx = useContext(LocaleContext);
  if (!ctx) throw new Error("useAppLocale must be used within LocaleProvider");
  return ctx;
}
