"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { apiFetch, ApiError } from "@/lib/api/client";
import { formatBirthDdMmYyyy } from "@/lib/dates";
import type { Gender } from "@/lib/api/types";

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [fullname, setFullname] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [gender, setGender] = useState<Gender>("MALE");
  const [birth, setBirth] = useState(""); // yyyy-mm-dd from input type=date
  const [address, setAddress] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  function setPhoneDigits(value: string) {
    const digits = value.replace(/\D/g, "").slice(0, 11);
    setPhone(digits);
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      if (phone.length !== 11) {
        setError("Số điện thoại phải đúng 11 chữ số");
        setPending(false);
        return;
      }
      const birthFormatted = formatBirthDdMmYyyy(birth);
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
            phone: phone.trim(),
            gender,
            birth: birthFormatted,
            address: address.trim(),
            role: "CUSTOMER",
          },
        ]),
      });
      router.push("/login?registered=1");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Đăng ký thất bại");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-12 sm:px-6">
      <h1
        className="font-serif text-3xl font-semibold text-brand-900"
        style={{ fontFamily: "var(--font-cormorant), serif" }}
      >
        Đăng ký khách
      </h1>
      <p className="mt-2 text-sm text-muted">Tạo tài khoản để đặt món và đặt bàn khi đến nhà hàng.</p>

      <form onSubmit={onSubmit} className="mt-8 space-y-4 rounded-2xl border border-stone-200 bg-surface p-6 shadow-sm">
        {error ? (
          <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800 ring-1 ring-red-100">{error}</p>
        ) : null}

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Tên đăng nhập">
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={inputClass}
              required
              minLength={3}
              maxLength={50}
            />
          </Field>
          <Field label="Mật khẩu">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={inputClass}
              required
              minLength={6}
            />
          </Field>
        </div>
        <Field label="Họ tên">
          <input value={fullname} onChange={(e) => setFullname(e.target.value)} className={inputClass} required maxLength={100} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Email">
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass} required />
          </Field>
          <Field label="Điện thoại (11 số)">
            <input
              type="text"
              inputMode="numeric"
              autoComplete="tel"
              value={phone}
              onChange={(e) => setPhoneDigits(e.target.value)}
              className={inputClass}
              required
              minLength={11}
              maxLength={11}
              pattern="[0-9]{11}"
              placeholder=""
              title="Nhập đúng 11 chữ số"
            />
          </Field>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Giới tính">
            <select value={gender} onChange={(e) => setGender(e.target.value as Gender)} className={inputClass}>
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
            </select>
          </Field>
          <Field label="Ngày sinh">
            <input type="date" value={birth} onChange={(e) => setBirth(e.target.value)} className={inputClass} required />
          </Field>
        </div>
        <Field label="Địa chỉ">
          <input value={address} onChange={(e) => setAddress(e.target.value)} className={inputClass} required maxLength={255} />
        </Field>

        <button
          type="submit"
          disabled={pending}
          className="w-full rounded-xl bg-brand-800 py-3 text-sm font-semibold text-white shadow transition hover:bg-brand-900 disabled:opacity-60"
        >
          {pending ? "Đang gửi…" : "Đăng ký"}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-muted">
        Đã có tài khoản?{" "}
        <Link href="/login" className="font-semibold text-brand-800 hover:underline">
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}

const inputClass =
  "mt-1 w-full rounded-lg border border-stone-300 px-3 py-2 text-stone-900 shadow-sm outline-none focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20";

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <span className="block text-sm font-medium text-stone-700">{label}</span>
      {children}
    </div>
  );
}
