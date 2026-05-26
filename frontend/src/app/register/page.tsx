"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import { BirthDateInput } from "@/components/BirthDateInput";
import { AuthSplitLayout } from "@/components/auth/AuthSplitLayout";
import {
  AuthAlert,
  AuthFieldGroup,
  AuthFooterLink,
  AuthLabeledInput,
  AuthLabeledPassword,
  AuthPageHeader,
  AuthSubmitButton,
} from "@/components/auth/AuthFormFields";
import { authBirthSegmentClass, authGridRowClass, authSelectClass } from "@/components/auth/auth-styles";
import { IconLock, IconMail, IconUser } from "@/components/auth/AuthIcons";
import { PhoneNationalInput } from "@/components/PhoneNationalInput";
import { resolveBirthIsoFromField, validateBirthField } from "@/lib/birth";
import { formatBirthDdMmYyyy } from "@/lib/dates";
import type { Gender } from "@/lib/api/types";
import { isAllowedUserEmail, USER_EMAIL_DOMAIN_MESSAGE } from "@/lib/email";
import {
  isValidPhoneDigits,
  normalizeVietnamMobilePhone,
  PHONE_VALIDATION_MESSAGE,
} from "@/lib/phone";

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [fullname, setFullname] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [gender, setGender] = useState<Gender>("MALE");
  const [birth, setBirth] = useState("");
  const [birthFieldError, setBirthFieldError] = useState<string | null>(null);
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      if (!isValidPhoneDigits(phone)) {
        setError(PHONE_VALIDATION_MESSAGE);
        setPending(false);
        return;
      }
      if (!isAllowedUserEmail(email.trim())) {
        setError(USER_EMAIL_DOMAIN_MESSAGE);
        setPending(false);
        return;
      }
      const birthError = validateBirthField(birth);
      if (birthError) {
        setError(birthError);
        setPending(false);
        return;
      }
      const birthFormatted = formatBirthDdMmYyyy(resolveBirthIsoFromField(birth));
      if (!birthFormatted) {
        setError("Chọn ngày sinh hợp lệ");
        setPending(false);
        return;
      }
      await apiFetch<unknown[]>("/users/register", {
        auth: false,
        method: "POST",
        body: JSON.stringify([
          {
            username: username.trim(),
            password,
            fullname: fullname.trim(),
            email: email.trim(),
            phone: normalizeVietnamMobilePhone(phone),
            gender,
            birth: birthFormatted,
            address: address.trim(),
            role: "CUSTOMER",
          },
        ]),
      });
      router.push("/login?registered=1&email=" + encodeURIComponent(email.trim()));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Đăng ký thất bại");
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthSplitLayout wide>
      <AuthPageHeader
        title="Đăng ký tài khoản"
        subtitle="Tạo tài khoản khách để đặt món và đặt bàn tại Bistro."
      />

      <form onSubmit={onSubmit} className="space-y-5">
        {error ? <AuthAlert variant="error">{error}</AuthAlert> : null}

        <div className={authGridRowClass}>
          <AuthLabeledInput
            id="reg-username"
            label="Tên đăng nhập"
            icon={<IconUser className="h-5 w-5" />}
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            minLength={3}
            maxLength={50}
          />
          <AuthLabeledPassword
            id="reg-password"
            label="Mật khẩu"
            icon={<IconLock className="h-5 w-5" />}
            value={password}
            onChange={setPassword}
            required
            minLength={6}
          />
        </div>

        <AuthLabeledInput
          id="reg-fullname"
          label="Họ và tên"
          value={fullname}
          onChange={(e) => setFullname(e.target.value)}
          required
          maxLength={100}
        />

        <div className={authGridRowClass}>
          <AuthLabeledInput
            id="reg-email"
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
          id="reg-address"
          label="Địa chỉ"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          required
          maxLength={255}
        />

        <AuthSubmitButton pending={pending} pendingLabel="Đang gửi…" disabled={birthFieldError != null}>
          Đăng ký
        </AuthSubmitButton>
      </form>

      <AuthFooterLink>
        Đã có tài khoản?{" "}
        <Link href="/login" className="font-semibold text-brand-800 hover:underline">
          Đăng nhập
        </Link>
      </AuthFooterLink>
    </AuthSplitLayout>
  );
}
