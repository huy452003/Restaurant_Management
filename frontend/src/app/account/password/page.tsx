"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/context/auth-context";
import { AccountChangePasswordForm } from "@/components/account/AccountChangePasswordForm";
import { AuthCenteredLayout } from "@/components/auth/AuthCenteredLayout";
import { AuthFooterLink, AuthPageHeader } from "@/components/auth/AuthFormFields";

export default function AccountPasswordPage() {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace("/login?next=/account/password");
      return;
    }
    if (user.role !== "CUSTOMER") {
      router.replace("/menu");
    }
  }, [user, loading, router]);

  if (loading || !user) {
    return (
      <div className="flex min-h-[calc(100dvh-4rem)] items-center justify-center text-muted lg:min-h-[calc(100dvh-8rem)]">
        Đang tải…
      </div>
    );
  }

  if (user.role !== "CUSTOMER") {
    return null;
  }

  return (
    <AuthCenteredLayout>
      <AuthPageHeader
        title="Đổi mật khẩu"
        subtitle={`Khách hàng · ${user.username}`}
      />

      <AccountChangePasswordForm userId={user.id!} />

      <AuthFooterLink>
        <Link href="/account" className="font-semibold text-brand-800 hover:underline">
          ← Quay lại thông tin tài khoản
        </Link>
      </AuthFooterLink>
    </AuthCenteredLayout>
  );
}
