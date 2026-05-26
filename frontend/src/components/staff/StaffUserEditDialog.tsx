"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { Gender, UserModel, UserRole } from "@/lib/api/types";
import { BirthDateInput } from "@/components/BirthDateInput";
import {
  AuthAlert,
  AuthFieldGroup,
  AuthLabeledInput,
  AuthPageHeader,
} from "@/components/auth/AuthFormFields";
import {
  authBirthSegmentClass,
  authGridRowClass,
  authPrimaryButton,
  authSecondaryButton,
  authSelectClass,
} from "@/components/auth/auth-styles";
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

const ROLES: UserRole[] = ["ADMIN", "CUSTOMER", "MANAGER", "CASHIER"];
const STATUSES = ["ACTIVE", "INACTIVE", "PENDING"] as const;

type Props = {
  row: UserModel | null;
  onClose: () => void;
  onSaved: () => void;
};

export function StaffUserEditDialog({ row, onClose, onSaved }: Props) {
  const [username, setUsername] = useState("");
  const [fullname, setFullname] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [gender, setGender] = useState<Gender>("MALE");
  const [birthInput, setBirthInput] = useState("");
  const [birthFieldError, setBirthFieldError] = useState<string | null>(null);
  const [address, setAddress] = useState("");
  const [role, setRole] = useState<UserRole>("CUSTOMER");
  const [userStatus, setUserStatus] = useState<string>("ACTIVE");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!row) return;
    void Promise.resolve().then(() => {
      setFullname(row.fullname ?? "");
      setEmail(row.email ?? "");
      setPhone(row.phone ?? "");
      setGender((row.gender as Gender) || "MALE");
      setBirthInput(birthDdMmYyyyToInputDate(row.birth) || "");
      setAddress(row.address ?? "");
      setRole(row.role);
      setUserStatus(row.userStatus ?? "ACTIVE");
      setPassword("");
      setError(null);
      setBirthFieldError(null);
    });
  }, [row]);

  if (!row) return null;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!row) return;
    setError(null);
    const birthError = validateBirthField(birthInput);
    if (birthError) {
      setError(birthError);
      return;
    }
    const birthFormatted = formatBirthDdMmYyyy(resolveBirthIsoFromField(birthInput));
    if (!birthFormatted) {
      setError("Ngày sinh không hợp lệ");
      return;
    }
    const pw = password.trim();
    if (pw.length > 0 && pw.length < 6) {
      setError("Mật khẩu mới tối thiểu 6 ký tự");
      return;
    }
    if (!isAllowedUserEmail(email.trim())) {
      setError(USER_EMAIL_DOMAIN_MESSAGE);
      return;
    }
    if (!isValidPhoneDigits(phone)) {
      setError(PHONE_VALIDATION_MESSAGE);
      return;
    }
    setPending(true);
    try {
      const update: Record<string, unknown> = {
        username: row.username,
        fullname: fullname.trim(),
        email: email.trim(),
        phone: normalizeVietnamMobilePhone(phone),
        gender,
        birth: birthFormatted,
        address: address.trim(),
        role,
        userStatus,
      };
      if (pw.length > 0) update.password = pw;

      await apiFetch<UserModel[]>("/users", {
        method: "PUT",
        body: JSON.stringify({
          ids: [row.id],
          updates: [update],
        }),
      });
      onSaved();
      onClose();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Cập nhật thất bại");
    } finally {
      setPending(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl bg-white px-6 py-8 shadow-xl ring-1 ring-stone-200/80 sm:px-8"
        role="dialog"
        aria-labelledby="edit-user-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <AuthPageHeader
          title={`Sửa người dùng #${row.id}`}
          subtitle={`${row.username} · Cập nhật thông tin và quyền truy cập.`}
        />

        <form onSubmit={handleSubmit} className="space-y-5">
          {error ? <AuthAlert variant="error">{error}</AuthAlert> : null}

          <AuthLabeledInput
            id="staff-edit-fullname"
            label="Họ và tên"
            value={fullname}
            onChange={(e) => setFullname(e.target.value)}
            required
            maxLength={100}
          />

          <div className={authGridRowClass}>
            <AuthLabeledInput
              id="staff-edit-email"
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
                value={birthInput}
                onChange={setBirthInput}
                onValidationChange={setBirthFieldError}
                layout="fill"
                className={authBirthSegmentClass}
                required
              />
            </AuthFieldGroup>
          </div>

          <AuthLabeledInput
            id="staff-edit-address"
            label="Địa chỉ"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            required
            maxLength={255}
          />

          <AuthLabeledInput
            id="staff-edit-password"
            label="Mật khẩu mới"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Để trống = giữ nguyên"
            maxLength={100}
          />

          <div className={authGridRowClass}>
            <AuthFieldGroup label="Vai trò">
              <select value={role} onChange={(e) => setRole(e.target.value as UserRole)} className={authSelectClass}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {STAFF_ROLE_LABEL_VI[r]}
                  </option>
                ))}
              </select>
            </AuthFieldGroup>
            <AuthFieldGroup label="Trạng thái">
              <select value={userStatus} onChange={(e) => setUserStatus(e.target.value)} className={authSelectClass}>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </AuthFieldGroup>
          </div>

          <div className="flex flex-wrap justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className={authSecondaryButton}>
              Hủy
            </button>
            <button
              type="submit"
              disabled={pending || birthFieldError != null}
              className={`${authPrimaryButton} w-auto px-6 normal-case tracking-normal`}
            >
              {pending ? "Đang lưu…" : "Lưu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
