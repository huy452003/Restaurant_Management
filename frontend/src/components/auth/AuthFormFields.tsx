"use client";

import { useState, type InputHTMLAttributes, type ReactNode } from "react";
import {
  authControlMinH,
  authFieldShell,
  authIconSlot,
  authPrimaryButton,
} from "@/components/auth/auth-styles";
import { IconEye, IconEyeOff } from "@/components/auth/AuthIcons";

function hasInputValue(value: string | number | readonly string[] | undefined): boolean {
  if (value == null) return false;
  return String(value).length > 0;
}

function floatingLabelClass(floated: boolean, focused: boolean, labelLeft: string): string {
  const base = `pointer-events-none absolute z-10 origin-left font-medium transition-all duration-150 ${labelLeft}`;
  if (floated) {
    return `${base} top-2 text-xs ${focused ? "text-brand-700" : "text-stone-500"}`;
  }
  return `${base} top-1/2 -translate-y-1/2 text-sm text-stone-400`;
}

function inputPadClass(floated: boolean, padLeft: string, padRight = "pr-4"): string {
  const vert = floated ? "pt-6 pb-2.5" : "py-3.5";
  return `${authFieldShell} ${vert} ${padLeft} ${padRight}`;
}

export function AuthPageHeader({ title, subtitle }: { title: string; subtitle: string }) {
  return (
    <header className="mb-8">
      <h1
        className="font-serif text-3xl font-bold text-brand-900 sm:text-[2rem]"
        style={{ fontFamily: "var(--font-cormorant), serif" }}
      >
        {title}
      </h1>
      <p className="mt-2 text-sm leading-relaxed text-muted">{subtitle}</p>
    </header>
  );
}

export function AuthFloatingInput({
  id,
  label,
  icon,
  className = "",
  value,
  onFocus,
  onBlur,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  icon?: ReactNode;
}) {
  const [focused, setFocused] = useState(false);
  const floated = focused || hasInputValue(value);
  const padLeft = icon ? "pl-10" : "pl-4";
  const labelLeft = icon ? "left-10" : "left-4";

  return (
    <div className="relative">
      {icon ? <span className={authIconSlot}>{icon}</span> : null}
      <input
        id={id}
        value={value}
        className={`${inputPadClass(floated, padLeft)} ${className}`}
        onFocus={(e) => {
          setFocused(true);
          onFocus?.(e);
        }}
        onBlur={(e) => {
          setFocused(false);
          onBlur?.(e);
        }}
        {...props}
      />
      <label htmlFor={id} className={floatingLabelClass(floated, focused, labelLeft)}>
        {label}
      </label>
    </div>
  );
}

export function AuthFloatingPassword({
  id,
  label,
  icon,
  value,
  onChange,
  autoComplete,
  minLength,
  required,
}: {
  id: string;
  label: string;
  icon?: ReactNode;
  value: string;
  onChange: (v: string) => void;
  autoComplete?: string;
  minLength?: number;
  required?: boolean;
}) {
  const [visible, setVisible] = useState(false);
  const [focused, setFocused] = useState(false);
  const floated = focused || hasInputValue(value);

  return (
    <div className="relative">
      {icon ? <span className={authIconSlot}>{icon}</span> : null}
      <input
        id={id}
        type={visible ? "text" : "password"}
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        required={required}
        minLength={minLength}
        className={inputPadClass(floated, "pl-10", "pr-11")}
      />
      <label htmlFor={id} className={floatingLabelClass(floated, focused, "left-10")}>
        {label}
      </label>
      <button
        type="button"
        onClick={() => setVisible((v) => !v)}
        className="absolute right-3 top-[1.35rem] z-10 -translate-y-1/2 rounded-lg p-1 text-stone-400 hover:bg-stone-200/80 hover:text-stone-600"
        aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
        tabIndex={-1}
      >
        {visible ? <IconEyeOff className="h-5 w-5" /> : <IconEye className="h-5 w-5" />}
      </button>
    </div>
  );
}

export function AuthFieldGroup({
  label,
  hint,
  children,
  className = "",
}: {
  label: string;
  hint?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <span className="text-xs font-medium text-stone-500">{label}</span>
      <div className={authControlMinH}>{children}</div>
      {hint ? <p className="text-xs text-stone-400">{hint}</p> : null}
    </div>
  );
}

/** @deprecated Use AuthFieldGroup */
export const AuthStaticField = AuthFieldGroup;

const labeledInputClass = (icon?: ReactNode) =>
  `${authFieldShell} ${authControlMinH} w-full py-0 ${icon ? "pl-10 pr-4" : "px-4"}`;

export function AuthLabeledInput({
  id,
  label,
  icon,
  className = "",
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  icon?: ReactNode;
}) {
  return (
    <AuthFieldGroup label={label} className={className}>
      <div className="relative h-full">
        {icon ? (
          <span className="pointer-events-none absolute left-3.5 top-1/2 z-10 -translate-y-1/2 text-stone-400">
            {icon}
          </span>
        ) : null}
        <input id={id} className={labeledInputClass(icon)} {...props} />
      </div>
    </AuthFieldGroup>
  );
}

export function AuthLabeledPassword({
  id,
  label,
  icon,
  value,
  onChange,
  autoComplete,
  minLength,
  required,
  className = "",
}: {
  id: string;
  label: string;
  icon?: ReactNode;
  value: string;
  onChange: (v: string) => void;
  autoComplete?: string;
  minLength?: number;
  required?: boolean;
  className?: string;
}) {
  const [visible, setVisible] = useState(false);

  return (
    <AuthFieldGroup label={label} className={className}>
      <div className="relative h-full">
        {icon ? (
          <span className="pointer-events-none absolute left-3.5 top-1/2 z-10 -translate-y-1/2 text-stone-400">
            {icon}
          </span>
        ) : null}
        <input
          id={id}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          required={required}
          minLength={minLength}
          className={`${labeledInputClass(icon)} pr-11`}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          className="absolute right-3 top-1/2 z-10 -translate-y-1/2 rounded-lg p-1 text-stone-400 hover:bg-stone-200/80 hover:text-stone-600"
          aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          tabIndex={-1}
        >
          {visible ? <IconEyeOff className="h-5 w-5" /> : <IconEye className="h-5 w-5" />}
        </button>
      </div>
    </AuthFieldGroup>
  );
}

export function AuthAlert({ variant, children }: { variant: "error" | "success"; children: ReactNode }) {
  const cls =
    variant === "error"
      ? "bg-red-50 text-red-800 ring-red-100"
      : "bg-emerald-50 text-emerald-800 ring-emerald-100";
  return (
    <p className={`rounded-xl px-4 py-3 text-sm ring-1 ${cls}`} role="alert">
      {children}
    </p>
  );
}

export function AuthSubmitButton({
  pending,
  pendingLabel,
  children,
  disabled,
}: {
  pending: boolean;
  pendingLabel: string;
  children: ReactNode;
  disabled?: boolean;
}) {
  return (
    <button type="submit" disabled={pending || disabled} className={authPrimaryButton}>
      {pending ? pendingLabel : children}
    </button>
  );
}

export function AuthFooterLink({ children }: { children: ReactNode }) {
  return <p className="mt-8 text-center text-sm text-stone-500">{children}</p>;
}
