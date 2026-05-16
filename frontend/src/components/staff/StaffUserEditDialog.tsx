"use client";

import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import type { Gender, UserModel, UserRole } from "@/lib/api/types";
import { birthDdMmYyyyToInputDate, formatBirthDdMmYyyy } from "@/lib/dates";

import { STAFF_ROLE_LABEL_VI } from "@/lib/staff/role-labels";

const ROLES: UserRole[] = ["ADMIN", "CUSTOMER", "MANAGER", "CHEF", "CASHIER"];

const STATUSES = ["ACTIVE", "INACTIVE", "PENDING"] as const;

type Props = {
  row: UserModel | null;
  onClose: () => void;
  onSaved: () => void;
};

const fieldClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

export function StaffUserEditDialog({ row, onClose, onSaved }: Props) {
  const [username, setUsername] = useState("");
  const [fullname, setFullname] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [gender, setGender] = useState<Gender>("MALE");
  const [birthInput, setBirthInput] = useState("");
  const [address, setAddress] = useState("");
  const [role, setRole] = useState<UserRole>("CUSTOMER");
  const [userStatus, setUserStatus] = useState<string>("ACTIVE");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!row) return;
    void Promise.resolve().then(() => {
      setUsername(row.username ?? "");
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
    });
  }, [row]);

  if (!row) return null;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!row) return;
    setError(null);
    const birthFormatted = formatBirthDdMmYyyy(birthInput);
    if (!birthFormatted) {
      setError("Ngày sinh không hợp lệ");
      return;
    }
    const pw = password.trim();
    if (pw.length > 0 && pw.length < 6) {
      setError("Mật khẩu mới tối thiểu 6 ký tự");
      return;
    }
    setPending(true);
    try {
      const update: Record<string, unknown> = {
        username: username.trim(),
        fullname: fullname.trim(),
        email: email.trim(),
        phone: phone.trim(),
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
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-stone-200 bg-surface p-6 shadow-xl"
        role="dialog"
        aria-labelledby="edit-user-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <h2 id="edit-user-title" className="font-serif text-xl font-semibold text-brand-900">
          Sửa người dùng #{row.id}
        </h2>

        <form onSubmit={handleSubmit} className="mt-6 space-y-3">
          {error ? <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p> : null}

          <label className="block text-xs font-medium text-stone-600">
            Tên đăng nhập
            <input className={fieldClass} value={username} onChange={(e) => setUsername(e.target.value)} required minLength={3} maxLength={50} />
          </label>
          <label className="block text-xs font-medium text-stone-600">
            Họ tên
            <input className={fieldClass} value={fullname} onChange={(e) => setFullname(e.target.value)} required maxLength={100} />
          </label>
          <label className="block text-xs font-medium text-stone-600">
            Email
            <input type="email" className={fieldClass} value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="block text-xs font-medium text-stone-600">
            SĐT (10–11 số)
            <input className={fieldClass} value={phone} onChange={(e) => setPhone(e.target.value.replace(/\D/g, "").slice(0, 11))} required pattern="[0-9]{10,11}" />
          </label>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-medium text-stone-600">
              Giới tính
              <select className={fieldClass} value={gender} onChange={(e) => setGender(e.target.value as Gender)}>
                <option value="MALE">Nam</option>
                <option value="FEMALE">Nữ</option>
              </select>
            </label>
            <label className="block text-xs font-medium text-stone-600">
              Ngày sinh
              <input type="date" className={fieldClass} value={birthInput} onChange={(e) => setBirthInput(e.target.value)} required />
            </label>
          </div>
          <label className="block text-xs font-medium text-stone-600">
            Địa chỉ
            <input className={fieldClass} value={address} onChange={(e) => setAddress(e.target.value)} required maxLength={255} />
          </label>
          <label className="block text-xs font-medium text-stone-600">
            Mật khẩu mới
            <input
              type="password"
              autoComplete="new-password"
              className={fieldClass}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Để trống = giữ nguyên"
              maxLength={100}
            />
          </label>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-xs font-medium text-stone-600">
              Vai trò
              <select className={fieldClass} value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
                {ROLES.map((r) => (
                  <option key={r} value={r}>
                    {STAFF_ROLE_LABEL_VI[r]}
                  </option>
                ))}
              </select>
            </label>
            <label className="block text-xs font-medium text-stone-600">
              Trạng thái
              <select className={fieldClass} value={userStatus} onChange={(e) => setUserStatus(e.target.value)}>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="flex flex-wrap justify-end gap-3 pt-4">
            <button type="button" onClick={onClose} className="rounded-lg border border-stone-300 px-4 py-2 text-sm font-medium hover:bg-stone-50">
              Hủy
            </button>
            <button
              type="submit"
              disabled={pending}
              className="rounded-lg bg-brand-800 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-900 disabled:opacity-50"
            >
              {pending ? "Đang lưu…" : "Lưu"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
