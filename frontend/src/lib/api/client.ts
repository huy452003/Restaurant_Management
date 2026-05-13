import { notifyJwtExpired } from "@/lib/auth/jwt-expired";
import { acceptLanguageHeader, getStoredAppLocale } from "@/lib/locale";
import type { ApiResponse } from "./types";

const API_BASE = "/api/proxy";

export class ApiError extends Error {
  status: number;
  body: unknown;
  constructor(message: string, status: number, body?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem("accessToken");
}

function pickStr(obj: Record<string, unknown>, key: string): string {
  const v = obj[key];
  return typeof v === "string" ? v.trim() : "";
}

const ERRORS_SKIP_KEYS = new Set([
  "notFoundItems",
  "conflictItems",
  "invalidFieldRefs",
  "invalidReferences",
  "Summary",
  "summary",
]);

/** Khóa trong `errors` mà giá trị string thường là tiếng Anh từ exception, không phải bản dịch theo Accept-Language. */
const ERRORS_SUMMARY_STRING_KEYS = new Set([
  "Error",
  "error",
  "Required",
  "required",
  "detailMessage",
  "detail",
]);

function errorsHasFieldLikeStringMessages(e: Record<string, unknown>): boolean {
  for (const [k, v] of Object.entries(e)) {
    if (ERRORS_SKIP_KEYS.has(k)) continue;
    if (ERRORS_SUMMARY_STRING_KEYS.has(k)) continue;
    if (typeof v === "string" && v.trim()) return true;
  }
  return false;
}

/**
 * Lấy nội dung lỗi hiển thị cho người dùng.
 *
 * - Map validation (field → message): ghép từ `errors`.
 * - Forbidden / NotFound / …: backend đặt bản dịch ở `message` (theo Accept-Language); `errors.Error` / `detailMessage`
 *   thường là tiếng Anh từ `getMessage()` — ưu tiên `message` khi không có lỗi theo field.
 */
export function parseApiErrorMessage(json: unknown, statusText: string): string {
  if (typeof json !== "object" || json === null) return statusText;
  const o = json as Record<string, unknown>;

  // Phản hồi lỗi Spring chuẩn (không bọc Response)
  if (typeof o.status === "number" && typeof o.message === "string" && o.message.trim()) {
    const m = o.message.trim();
    if (m && !("modelName" in o)) return m;
  }

  const errs = o.errors;
  if (errs && typeof errs === "object" && errs !== null) {
    const e = errs as Record<string, unknown>;

    if (errorsHasFieldLikeStringMessages(e)) {
      const parts: string[] = [];
      for (const [k, v] of Object.entries(e)) {
        if (ERRORS_SKIP_KEYS.has(k)) continue;
        if (typeof v === "string" && v.trim()) parts.push(`${k}: ${v.trim()}`);
      }
      if (parts.length) return parts.slice(0, 8).join(" · ");
    }

    const localized = typeof o.message === "string" ? o.message.trim() : "";
    if (localized) return localized;

    const primary =
      pickStr(e, "Error") ||
      pickStr(e, "error") ||
      pickStr(e, "detailMessage") ||
      pickStr(e, "detail");

    const req = pickStr(e, "Required") || pickStr(e, "required");
    if (primary) return req ? `${primary} — ${req}` : primary;
  }

  if (typeof o.message === "string" && o.message.trim()) return o.message.trim();
  return statusText;
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit & { auth?: boolean } = {},
): Promise<ApiResponse<T>> {
  const { auth = true, headers: initHeaders, ...rest } = options;
  const headers = new Headers(initHeaders);
  if (!headers.has("Content-Type") && rest.body && !(rest.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("Accept-Language")) {
    headers.set("Accept-Language", acceptLanguageHeader(getStoredAppLocale()));
  }
  if (auth) {
    const token = getStoredToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(`${API_BASE}${path.startsWith("/") ? path : `/${path}`}`, {
    ...rest,
    headers,
  });

  const text = await res.text();
  let json: unknown = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    throw new ApiError("Phản hồi không phải JSON", res.status, text);
  }

  if (!res.ok) {
    if (res.status === 401 && auth) {
      notifyJwtExpired();
    }
    const msg = parseApiErrorMessage(json, res.statusText);
    throw new ApiError(msg || "Lỗi mạng", res.status, json);
  }

  return json as ApiResponse<T>;
}

export function buildPageParams(
  page: number,
  size: number,
  extra?: Record<string, string | number | undefined | null>,
): string {
  const p = new URLSearchParams();
  p.set("page", String(page));
  p.set("size", String(size));
  if (extra) {
    for (const [k, v] of Object.entries(extra)) {
      if (v !== undefined && v !== null && v !== "") p.set(k, String(v));
    }
  }
  return p.toString();
}
