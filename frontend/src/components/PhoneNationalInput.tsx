"use client";

import { authControlMinH } from "@/components/auth/auth-styles";
import {
  PHONE_LOCAL_MAX_LENGTH,
  PHONE_VALIDATION_MESSAGE,
  phoneToLocalDisplay,
  sanitizePhoneLocalInput,
} from "@/lib/phone";

type Props = {
  value: string;
  onChange: (localDigits: string) => void;
  className?: string;
  inputClassName?: string;
  /** Nền xám auth form (split layout) */
  variant?: "default" | "auth";
  required?: boolean;
  disabled?: boolean;
  id?: string;
};

export function PhoneNationalInput({
  value,
  onChange,
  className = "",
  inputClassName = "",
  variant = "default",
  required,
  disabled,
  id,
}: Props) {
  const local = phoneToLocalDisplay(value);
  const isAuth = variant === "auth";

  if (isAuth) {
    return (
      <div
        className={`flex overflow-hidden rounded-xl bg-stone-100 shadow-sm transition focus-within:bg-white focus-within:ring-2 focus-within:ring-brand-600/20 ${authControlMinH} ${className}`}
      >
        <span
          className={`inline-flex shrink-0 items-center border-r border-stone-200/80 px-3 text-sm font-semibold text-stone-600 ${disabled ? "opacity-60" : ""}`}
          aria-hidden
        >
          +84
        </span>
        <input
          id={id}
          type="text"
          inputMode="numeric"
          autoComplete="tel-national"
          value={local}
          onChange={(e) => onChange(sanitizePhoneLocalInput(e.target.value))}
          className={`min-w-0 flex-1 border-0 bg-transparent px-3 py-0 text-stone-900 outline-none disabled:cursor-not-allowed disabled:opacity-60 ${inputClassName}`}
          required={required}
          disabled={disabled}
          maxLength={PHONE_LOCAL_MAX_LENGTH}
          title={PHONE_VALIDATION_MESSAGE}
        />
      </div>
    );
  }

  return (
    <div className={`flex ${className}`}>
      <span
        className={`inline-flex shrink-0 items-center rounded-l-lg border border-r-0 border-stone-300 bg-stone-100 px-3 text-sm font-medium text-stone-700 ${disabled ? "opacity-60" : ""}`}
        aria-hidden
      >
        +84
      </span>
      <input
        id={id}
        type="text"
        inputMode="numeric"
        autoComplete="tel-national"
        value={local}
        onChange={(e) => onChange(sanitizePhoneLocalInput(e.target.value))}
        className={`min-w-0 flex-1 rounded-r-lg border border-stone-300 px-3 py-2 text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20 disabled:cursor-not-allowed disabled:bg-stone-50 ${inputClassName}`}
        required={required}
        disabled={disabled}
        maxLength={PHONE_LOCAL_MAX_LENGTH}
        title={PHONE_VALIDATION_MESSAGE}
      />
    </div>
  );
}
