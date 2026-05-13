import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="mt-auto border-t border-stone-200 bg-stone-900 text-stone-300">
      <div className="mx-auto grid max-w-6xl gap-8 px-4 py-12 sm:grid-cols-2 sm:px-6 lg:grid-cols-3">
        <div>
          <p
            className="font-serif text-xl font-semibold text-white"
            style={{ fontFamily: "var(--font-cormorant), serif" }}
          >
            Bistro
          </p>
          <p className="mt-2 text-sm leading-relaxed text-stone-400">
            Hương vị theo mùa, phục vụ tận tâm.
          </p>
        </div>
        <div>
          <p className="text-sm font-semibold uppercase tracking-wider text-stone-500">
            Liên kết
          </p>
          <ul className="mt-3 space-y-2 text-sm">
            <li>
              <Link href="/menu" className="hover:text-white">
                Thực đơn
              </Link>
            </li>
            <li>
              <Link href="/reservations" className="hover:text-white">
                Đặt bàn
              </Link>
            </li>
            <li>
              <Link href="/login" className="hover:text-white">
                Đăng nhập
              </Link>
            </li>
          </ul>
        </div>
        <div>
          <p className="text-sm font-semibold uppercase tracking-wider text-stone-500">
            Giờ mở cửa
          </p>
          <p className="mt-3 text-sm text-stone-400">
            T2 — CN: 11:00 — 22:00
            <br />
            Hotline: 1900 0000
          </p>
        </div>
      </div>
      <div className="border-t border-stone-800 py-4 text-center text-xs text-stone-500">
        © {new Date().getFullYear()} HuyK3 Restaurant.
      </div>
    </footer>
  );
}
