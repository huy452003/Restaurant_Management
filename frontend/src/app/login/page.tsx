"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { AuthSplitLayout } from "@/components/auth/AuthSplitLayout";
import {
  AuthAlert,
  AuthFloatingInput,
  AuthFloatingPassword,
  AuthFooterLink,
  AuthPageHeader,
  AuthSubmitButton,
} from "@/components/auth/AuthFormFields";
import { IconLock, IconUser } from "@/components/auth/AuthIcons";
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

function LoginForm() {
  const { login } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const registered = searchParams.get("registered") === "1";

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await login(username.trim(), password);
      const afterLogout =
        typeof window !== "undefined" && window.sessionStorage.getItem("postLogoutLogin") === "1";
      if (typeof window !== "undefined") {
        window.sessionStorage.removeItem("postLogoutLogin");
      }
      router.push(afterLogout ? "/" : (readSafeNextPath() ?? "/"));
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Đăng nhập thất bại");
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthSplitLayout>
      <AuthPageHeader
        title="Đăng nhập Bistro"
        subtitle="Chào mừng trở lại! Dùng tài khoản bạn đã đăng ký để đặt món và đặt bàn."
      />

      <form onSubmit={onSubmit} className="space-y-5">
        {registered ? (
          <AuthAlert variant="success">
            Đăng ký thành công! Vui lòng kiểm tra hộp thư email để xác thực tài khoản trước khi đăng nhập.
          </AuthAlert>
        ) : null}
        {error ? <AuthAlert variant="error">{error}</AuthAlert> : null}

        <AuthFloatingInput
          id="username"
          label="Tên đăng nhập"
          icon={<IconUser className="h-5 w-5" />}
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          minLength={3}
        />

        <AuthFloatingPassword
          id="password"
          label="Mật khẩu"
          icon={<IconLock className="h-5 w-5" />}
          autoComplete="current-password"
          value={password}
          onChange={setPassword}
          required
          minLength={6}
        />

        <div className="flex items-center justify-between gap-3 text-sm">
          <label className="flex cursor-pointer items-center gap-2 text-stone-600">
            <input
              type="checkbox"
              checked={remember}
              onChange={(e) => setRemember(e.target.checked)}
              className="h-4 w-4 rounded border-stone-300 text-brand-800 focus:ring-brand-600/30"
            />
            Ghi nhớ đăng nhập
          </label>
        </div>

        <AuthSubmitButton pending={pending} pendingLabel="Đang xử lý…">
          Đăng nhập
        </AuthSubmitButton>
      </form>

      <AuthFooterLink>
        Chưa có tài khoản?{" "}
        <Link href="/register" className="font-semibold text-brand-800 hover:underline">
          Đăng ký
        </Link>
      </AuthFooterLink>
    </AuthSplitLayout>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<AuthSplitLayout wide={false}><p className="text-sm text-stone-500">Đang tải…</p></AuthSplitLayout>}>
      <LoginForm />
    </Suspense>
  );
}
