import Link from "next/link";

export function StaffBackLink() {
  return (
    <Link
      href="/staff"
      className="mb-6 inline-flex text-sm font-medium text-brand-800 underline-offset-2 hover:underline"
    >
      ← Khu quản lý
    </Link>
  );
}
