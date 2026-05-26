"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useRef, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import { AuthSplitLayout } from "@/components/auth/AuthSplitLayout";
import {
  AuthAlert,
  AuthFooterLink,
  AuthPageHeader,
  AuthSubmitButton,
} from "@/components/auth/AuthFormFields";

type VerifyState = "loading" | "success" | "error" | "no-token";

function VerifyContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [state, setState] = useState<VerifyState>(token ? "loading" : "no-token");
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const calledRef = useRef(false);

  useEffect(() => {
    if (!token || calledRef.current) return;
    calledRef.current = true;

    apiFetch<unknown>("/users/public/verify?verificationToken=" + encodeURIComponent(token), {
      auth: false,
      method: "PUT",
    })
      .then(() => setState("success"))
      .catch((err) => {
        setState("error");
        setErrorMsg(err instanceof ApiError ? err.message : "Xác thực thất bại. Vui lòng thử lại.");
      });
  }, [token]);

  if (state === "no-token") {
    return (
      <AuthSplitLayout>
        <AuthPageHeader
          title="Xác thực tài khoản"
          subtitle="Không tìm thấy mã xác thực. Vui lòng kiểm tra lại link trong email."
        />
        <AuthAlert variant="error">
          Link xác thực không hợp lệ hoặc thiếu mã token.
        </AuthAlert>
        <AuthFooterLink>
          <Link href="/register" className="font-semibold text-brand-800 hover:underline">
            Đăng ký lại
          </Link>
        </AuthFooterLink>
      </AuthSplitLayout>
    );
  }

  if (state === "loading") {
    return (
      <AuthSplitLayout>
        <AuthPageHeader
          title="Đang xác thực..."
          subtitle="Vui lòng chờ trong giây lát."
        />
        <div className="flex justify-center py-8">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-200 border-t-brand-700" />
        </div>
      </AuthSplitLayout>
    );
  }

  if (state === "success") {
    return (
      <AuthSplitLayout>
        <AuthPageHeader
          title="Xác thực thành công"
          subtitle="Tài khoản của bạn đã được kích hoạt. Bạn có thể đăng nhập ngay bây giờ."
        />
        <AuthAlert variant="success">
          Tài khoản đã kích hoạt thành công!
        </AuthAlert>
        <div className="mt-6">
          <AuthSubmitButton pending={false} pendingLabel="" disabled={false}>
            <Link href="/login" className="block w-full">
              Đăng nhập
            </Link>
          </AuthSubmitButton>
        </div>
      </AuthSplitLayout>
    );
  }

  return (
    <AuthSplitLayout>
      <AuthPageHeader
        title="Xác thực thất bại"
        subtitle="Không thể xác thực tài khoản. Link có thể đã hết hạn."
      />
      <AuthAlert variant="error">
        {errorMsg ?? "Đã xảy ra lỗi. Vui lòng thử lại."}
      </AuthAlert>
      <AuthFooterLink>
        Chưa nhận được email?{" "}
        <button
          type="button"
          onClick={() => router.push("/register")}
          className="font-semibold text-brand-800 hover:underline"
        >
          Đăng ký lại
        </button>
      </AuthFooterLink>
    </AuthSplitLayout>
  );
}

export default function VerifyPage() {
  return (
    <Suspense
      fallback={
        <AuthSplitLayout>
          <p className="text-sm text-stone-500">Đang tải...</p>
        </AuthSplitLayout>
      }
    >
      <VerifyContent />
    </Suspense>
  );
}
