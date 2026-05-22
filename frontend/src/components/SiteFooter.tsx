import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";
import { fontSerif } from "@/lib/ui/bakery";

const FOOTER_HEADING = "text-lg font-semibold text-accent";
const FOOTER_LINK = "text-sm text-brand-100/90 transition hover:text-accent";
const FOOTER_MUTED = "text-sm leading-relaxed text-brand-100/75";

function SocialIcon({
  href,
  label,
  children,
}: {
  href: string;
  label: string;
  children: ReactNode;
}) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={label}
      className="flex h-9 w-9 items-center justify-center rounded-full border border-accent/50 text-accent transition hover:border-accent hover:bg-accent/10 hover:text-white"
    >
      {children}
    </a>
  );
}

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="site-footer-bg mt-auto text-brand-100">
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 sm:py-14">
        <div className="flex flex-col gap-8 sm:flex-row sm:items-center sm:justify-between">
          <Link href="/" className="flex items-center gap-3 transition opacity-90 hover:opacity-100">
            <div className="relative h-12 w-12 overflow-hidden rounded-full ring-1 ring-accent/40">
              <Image src="/logo2.jpg" alt="" fill className="object-cover" sizes="48px" />
            </div>
            <span
              className="font-serif text-2xl font-semibold tracking-wide text-accent"
              style={fontSerif}
            >
              Bistro
            </span>
          </Link>

          <div className="flex flex-col gap-3 sm:items-end">
            <p className="text-sm font-medium text-brand-100/90">Theo dõi chúng tôi</p>
            <div className="flex gap-2.5">
              <SocialIcon href="https://facebook.com" label="Facebook">
                <FacebookIcon />
              </SocialIcon>
              <SocialIcon href="https://pinterest.com" label="Pinterest">
                <PinterestIcon />
              </SocialIcon>
              <SocialIcon href="https://wa.me" label="WhatsApp">
                <WhatsAppIcon />
              </SocialIcon>
              <SocialIcon href="https://instagram.com" label="Instagram">
                <InstagramIcon />
              </SocialIcon>
            </div>
          </div>
        </div>

        <div className="my-10 h-px bg-accent/35" aria-hidden />

        <div className="grid gap-10 sm:grid-cols-2 sm:gap-16">
          <div>
            <h2 className={FOOTER_HEADING} style={fontSerif}>
              Về chúng tôi
            </h2>
            <ul className={`mt-5 space-y-2 ${FOOTER_MUTED}`}>
              <li>
                <span className="text-accent/90">Điện thoại:</span> 1900 0000
              </li>
              <li>
                <span className="text-accent/90">Email:</span>{" "}
                <a href="mailto:huyk3@bistro.vn" className="transition hover:text-accent">
                  huyk3@bistro.vn
                </a>
              </li>
              <li>
                <span className="text-accent/90">Địa chỉ:</span> 123 Đường Ẩm Thực, Quận 0, TP. Hồ Chí Minh
              </li>
            </ul>
          </div>

          <div>
            <h2 className={FOOTER_HEADING} style={fontSerif}>
              Liên kết
            </h2>
            <ul className="mt-5 space-y-2.5">
              <li>
                <Link href="/" className={FOOTER_LINK}>
                  Trang chủ
                </Link>
              </li>
              <li>
                <Link href="/about" className={FOOTER_LINK}>
                  Về chúng tôi
                </Link>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div className="border-t border-accent/20 py-5 text-center text-xs text-brand-100/50">
        © {year} Bistro. All rights reserved.
      </div>
    </footer>
  );
}

function FacebookIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
    </svg>
  );
}

function PinterestIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 0C5.373 0 0 5.372 0 12c0 5.084 3.163 9.426 7.627 11.174-.105-.949-.2-2.405.042-3.441.219-.937 1.407-5.965 1.407-5.965s-.359-.719-.359-1.782c0-1.668.967-2.914 2.171-2.914 1.023 0 1.518.769 1.518 1.69 0 1.029-.655 2.568-.994 3.995-.283 1.194.599 2.169 1.777 2.169 2.133 0 3.772-2.249 3.772-5.495 0-2.873-2.064-4.882-5.012-4.882-3.414 0-5.418 2.561-5.418 5.207 0 1.031.397 2.138.893 2.738.098.119.112.224.083.345l-.333 1.36c-.053.22-.174.267-.402.161-1.499-.698-2.436-2.889-2.436-4.646 0-3.776 2.748-7.252 7.92-7.252 4.158 0 7.392 2.967 7.392 6.923 0 4.135-2.607 7.462-6.233 7.462-1.214 0-2.357-.629-2.746-1.378l-.748 2.853c-.271 1.043-1.004 2.35-1.494 3.146C9.57 23.812 10.763 24 12 24c6.627 0 12-5.373 12-12S18.627 0 12 0z" />
    </svg>
  );
}

function WhatsAppIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.435 9.884-9.881 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
    </svg>
  );
}

function InstagramIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 100 12.324 6.162 6.162 0 000-12.324zM12 16a4 4 0 110-8 4 4 0 010 8zm6.406-11.845a1.44 1.44 0 100 2.881 1.44 1.44 0 000-2.881z" />
    </svg>
  );
}
