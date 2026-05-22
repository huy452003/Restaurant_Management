"use client";

import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { UserModel } from "@/lib/api/types";
import {
  AuthAlert,
  AuthLabeledPassword,
  AuthSubmitButton,
} from "@/components/auth/AuthFormFields";
import { IconLock } from "@/components/auth/AuthIcons";

type Props = {
  userId: number;
};

export function AccountChangePasswordForm({ userId }: Props) {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (newPassword.length < 6) {
      setError("Mật khẩu mới tối thiểu 6 ký tự");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Mật khẩu mới và xác nhận không khớp");
      return;
    }
    if (newPassword === oldPassword) {
      setError("Mật khẩu mới phải khác mật khẩu hiện tại");
      return;
    }

    setPending(true);
    try {
      await apiFetch<UserModel>(`/users/${userId}/password`, {
        method: "PATCH",
        body: JSON.stringify({
          oldPassword,
          newPassword,
          confirmNewPassword: confirmPassword,
        }),
      });
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSuccess("Đã đổi mật khẩu thành công.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Không thể đổi mật khẩu");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="space-y-5">
        {error ? <AuthAlert variant="error">{error}</AuthAlert> : null}
        {success ? <AuthAlert variant="success">{success}</AuthAlert> : null}

        <AuthLabeledPassword
          id="account-old-password"
          label="Mật khẩu hiện tại"
          icon={<IconLock className="h-5 w-5" />}
          autoComplete="current-password"
          value={oldPassword}
          onChange={setOldPassword}
          required
          minLength={6}
        />
        <AuthLabeledPassword
          id="account-new-password"
          label="Mật khẩu mới"
          icon={<IconLock className="h-5 w-5" />}
          autoComplete="new-password"
          value={newPassword}
          onChange={setNewPassword}
          required
          minLength={6}
        />
        <AuthLabeledPassword
          id="account-confirm-password"
          label="Xác nhận mật khẩu mới"
          icon={<IconLock className="h-5 w-5" />}
          autoComplete="new-password"
          value={confirmPassword}
          onChange={setConfirmPassword}
          required
          minLength={6}
        />

      <AuthSubmitButton pending={pending} pendingLabel="Đang đổi…">
        Đổi mật khẩu
      </AuthSubmitButton>
    </form>
  );
}
