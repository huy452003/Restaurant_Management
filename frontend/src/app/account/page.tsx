"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/context/auth-context";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { Gender, UserModel } from "@/lib/api/types";
import { BirthDateInput } from "@/components/BirthDateInput";
import { AuthCenteredLayout } from "@/components/auth/AuthCenteredLayout";
import {
  AuthAlert,
  AuthFieldGroup,
  AuthLabeledInput,
  AuthFooterLink,
  AuthPageHeader,
  AuthSubmitButton,
} from "@/components/auth/AuthFormFields";
import { authBirthSegmentClass, authGridRowClass, authSelectClass } from "@/components/auth/auth-styles";
import { IconMail } from "@/components/auth/AuthIcons";
import { PhoneNationalInput } from "@/components/PhoneNationalInput";
import { resolveBirthIsoFromField, validateBirthField } from "@/lib/birth";
import { birthDdMmYyyyToInputDate, formatBirthDdMmYyyy } from "@/lib/dates";
import { isAllowedUserEmail, USER_EMAIL_DOMAIN_MESSAGE } from "@/lib/email";
import {
  isValidPhoneDigits,
  normalizeVietnamMobilePhone,
  PHONE_VALIDATION_MESSAGE,
} from "@/lib/phone";
import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";
export default function AccountPage() {
  const { user, loading, updateProfile } = useAuth();
  const router = useRouter();

  const [fullname, setFullname] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [gender, setGender] = useState<Gender>("MALE");
  const [birth, setBirth] = useState("");
  const [birthFieldError, setBirthFieldError] = useState<string | null>(null);
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace("/login?next=/account");
      return;
    }
    if (user.role !== "CUSTOMER") {
      router.replace("/menu");
    }
  }, [user, loading, router]);

  useEffect(() => {
    if (!user) return;
    setFullname(user.fullname ?? "");
    setEmail(user.email ?? "");
    setPhone(user.phone ?? "");
    setGender(user.gender ?? "MALE");
    setBirth(birthDdMmYyyyToInputDate(user.birth));
    setAddress(user.address ?? "");
  }, [user?.id]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.id) return;
    setError(null);
    setSuccess(null);

    if (!isValidPhoneDigits(phone)) {
      setError(PHONE_VALIDATION_MESSAGE);
      return;
    }
    if (!isAllowedUserEmail(email.trim())) {
      setError(USER_EMAIL_DOMAIN_MESSAGE);
      return;
    }
    const birthError = validateBirthField(birth);
    if (birthError) {
      setError(birthError);
      return;
    }
    const birthFormatted = formatBirthDdMmYyyy(resolveBirthIsoFromField(birth));
    if (!birthFormatted) {
      setError("Chọn ngày sinh hợp lệ");
      return;
    }

    setPending(true);
    try {
      const res = await apiFetch<UserModel>(`/users/${user.id}`, {
        method: "PATCH",
        body: JSON.stringify({
          fullname: fullname.trim(),
          email: email.trim(),
          phone: normalizeVietnamMobilePhone(phone),
          gender,
          birth: birthFormatted,
          address: address.trim(),
        }),
      });
      const updated = res.data;
      updateProfile({
        fullname: updated.fullname,
        email: updated.email,
        phone: updated.phone,
        gender: updated.gender,
        birth: updated.birth,
        address: updated.address,
      });
      setSuccess("Đã lưu thông tin tài khoản.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Không thể cập nhật tài khoản");
    } finally {
      setPending(false);
    }
  }

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
    <AuthCenteredLayout wide>
      <AuthPageHeader
        title="Thông tin tài khoản"
        subtitle={`${STAFF_ROLE_LABEL_VI[user.role]} · ${user.username}`}
      />

      <form onSubmit={onSubmit} className="space-y-5">
        {error ? <AuthAlert variant="error">{error}</AuthAlert> : null}
        {success ? <AuthAlert variant="success">{success}</AuthAlert> : null}

        <AuthLabeledInput
          id="account-fullname"
          label="Họ và tên"
          value={fullname}
          onChange={(e) => setFullname(e.target.value)}
          required
          maxLength={100}
        />

        <div className={authGridRowClass}>
          <AuthLabeledInput
            id="account-email"
            label="Email"
            icon={<IconMail className="h-5 w-5" />}
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <AuthFieldGroup label="Điện thoại">
            <PhoneNationalInput value={phone} onChange={setPhone} variant="auth" required />
          </AuthFieldGroup>
        </div>

        <div className={authGridRowClass}>
          <AuthFieldGroup label="Giới tính">
            <select
              value={gender}
              onChange={(e) => setGender(e.target.value as Gender)}
              className={authSelectClass}
            >
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
            </select>
          </AuthFieldGroup>
          <AuthFieldGroup label="Ngày sinh">
            <BirthDateInput
              value={birth}
              onChange={setBirth}
              onValidationChange={setBirthFieldError}
              layout="fill"
              className={authBirthSegmentClass}
              required
            />
          </AuthFieldGroup>
        </div>

        <AuthLabeledInput
          id="account-address"
          label="Địa chỉ"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          required
          maxLength={255}
        />

        <AuthSubmitButton pending={pending} pendingLabel="Đang lưu…" disabled={birthFieldError != null}>
          Lưu thay đổi
        </AuthSubmitButton>
      </form>

      <AuthFooterLink>
        <Link href="/account/password" className="font-semibold text-brand-800 hover:underline">
          Đổi mật khẩu
        </Link>
      </AuthFooterLink>
    </AuthCenteredLayout>
  );
}
