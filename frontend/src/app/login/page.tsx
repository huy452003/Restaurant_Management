"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/context/auth-context";
import { ApiError } from "@/lib/api/client";

function readSafeNextPath(): string | null {
  if (typeof window === "undefined") return null;
  const raw = new URLSearchParams(window.location.search).get("next");
  if (!raw) return null;
  try {
    const decoded = decodeURIComponent(raw);
    if (!decoded.startsWith("/") || decoded.startsWith("//")) return null;
    return decoded;
  } catch {
    return null;
  }
}

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await login(username.trim(), password);
      router.push(readSafeNextPath() ?? "/menu");
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Đăng nhập thất bại");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="mx-auto flex max-w-md flex-col gap-8 px-4 py-16 sm:px-6">
      <div>
        <h1
          className="font-serif text-3xl font-semibold text-brand-900"
          style={{ fontFamily: "var(--font-cormorant), serif" }}
        >
          Đăng nhập
        </h1>
        <p className="mt-2 text-sm text-muted">Dùng tài khoản từ hệ thống (JWT).</p>
      </div>

      <form onSubmit={onSubmit} className="space-y-4 rounded-2xl border border-stone-200 bg-surface p-6 shadow-sm">
        {error ? (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800 ring-1 ring-red-100">{error}</p>
        ) : null}
        <div>
          <label htmlFor="username" className="block text-sm font-medium text-stone-700">
            Tên đăng nhập
          </label>
          <input
            id="username"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-stone-900 shadow-sm outline-none ring-brand-600/0 transition focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20"
            required
            minLength={3}
          />
        </div>
        <div>
          <label htmlFor="password" className="block text-sm font-medium text-stone-700">
            Mật khẩu
          </label>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-stone-900 shadow-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20"
            required
            minLength={6}
          />
        </div>
        <button
          type="submit"
          disabled={pending}
          className="w-full rounded-xl bg-brand-800 py-3 text-sm font-semibold text-white shadow transition hover:bg-brand-900 disabled:opacity-60"
        >
          {pending ? "Đang xử lý…" : "Đăng nhập"}
        </button>
      </form>

      <p className="text-center text-sm text-muted">
        Chưa có tài khoản?{" "}
        <Link href="/register" className="font-semibold text-brand-800 underline-offset-2 hover:underline">
          Đăng ký
        </Link>
      </p>
    </div>
  );
}
