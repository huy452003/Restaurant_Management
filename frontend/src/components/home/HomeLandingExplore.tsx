"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { apiFetch, ApiError, buildPageParams } from "@/lib/api/client";
import type { MenuItemModel, PaginatedResponse } from "@/lib/api/types";
import { btnPrimaryClass, fontSerif } from "@/lib/ui/bakery";

const EXPLORE_TABS = [
  "Món chính",
  "Khai vị",
  "Tráng miệng",
] as const;

type ExploreTab = (typeof EXPLORE_TABS)[number];

type GalleryItem = {
  alt: string;
  src?: string;
  /** Nền gradient khi chưa có ảnh riêng */
  tone?: string;
};

const MAIN_DISH_GALLERY: GalleryItem[] = [
  { alt: "Món chính 1", src: "/c1.png" },
  { alt: "Món chính 2", src: "/c2.png" },
  { alt: "Món chính 3", src: "/c3.png" },
  { alt: "Món chính 4", src: "/c4.png" },
  { alt: "Món chính 5", src: "/c5.png" },
  { alt: "Món chính 6", src: "/c6.png" },
];

const APPETIZER_GALLERY: GalleryItem[] = [
  { alt: "Khai vị 1", src: "/k1.png" },
  { alt: "Khai vị 2", src: "/k2.png" },
  { alt: "Khai vị 3", src: "/k3.png" },
  { alt: "Khai vị 4", src: "/k4.png" },
  { alt: "Khai vị 5", src: "/k5.png" },
  { alt: "Khai vị 6", src: "/k6.png" },
];

const DESSERT_GALLERY: GalleryItem[] = [
  { alt: "Tráng miệng 1", src: "/t1.png" },
  { alt: "Tráng miệng 2", src: "/t2.png" },
  { alt: "Tráng miệng 3", src: "/t3.png" },
  { alt: "Tráng miệng 4", src: "/t4.png" },
  { alt: "Tráng miệng 5", src: "/t5.png" },
  { alt: "Tráng miệng 6", src: "/t6.png" },
];

const GALLERY_BY_TAB: Record<ExploreTab, GalleryItem[]> = {
  "Món chính": MAIN_DISH_GALLERY,
  "Khai vị": APPETIZER_GALLERY,
  "Tráng miệng": DESSERT_GALLERY,
};

const FEATURED_COUNT = 6;

const sectionTitleClass =
  "text-center font-serif text-3xl font-semibold tracking-tight text-brand-900 sm:text-4xl";

/** Lưới «Khám phá thêm» — cột đều, bo góc 4 cạnh (cover fill khung) */
const EXPLORE_GRID =
  "mx-auto mt-6 grid w-full max-w-5xl grid-cols-2 gap-3 px-4 sm:grid-cols-3 sm:px-6";
const EXPLORE_IMAGE_FRAME =
  "relative aspect-[5/4] min-h-0 w-full overflow-hidden rounded-2xl shadow-[0_4px_20px_-6px_rgba(74,55,40,0.12)]";
const EXPLORE_IMG = "rounded-2xl object-cover object-top";
const EXPLORE_IMAGE_SIZES = "(max-width: 640px) 46vw, (max-width: 1024px) 31vw, 300px";

/** «Món nổi bật» — lưới gọn hơn, bo góc rõ */
const FEATURED_GRID =
  "mx-auto mt-6 grid max-w-5xl grid-cols-2 gap-2.5 sm:grid-cols-3 sm:gap-3 lg:gap-3.5";
const FEATURED_IMAGE_FRAME =
  "relative aspect-[4/3] w-full overflow-hidden rounded-2xl bg-brand-50/30 shadow-[0_4px_20px_-6px_rgba(74,55,40,0.12)]";
const FEATURED_IMG = "object-cover object-center";
const FEATURED_IMAGE_SIZES = "(max-width: 640px) 44vw, (max-width: 1024px) 30vw, 280px";

const btnRectClass =
  "inline-flex items-center justify-center rounded-md bg-brand-800 px-8 py-2.5 text-sm font-semibold text-white shadow-md transition hover:bg-brand-900";

export function HomeLandingExplore() {
  const [activeTab, setActiveTab] = useState<ExploreTab>("Món chính");
  const [featured, setFeatured] = useState<MenuItemModel[]>([]);
  const [featuredLoading, setFeaturedLoading] = useState(false);
  const [featuredError, setFeaturedError] = useState<string | null>(null);
  const gallery = useMemo(() => GALLERY_BY_TAB[activeTab], [activeTab]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setFeaturedLoading(true);
      setFeaturedError(null);
      try {
        const qs = buildPageParams(0, FEATURED_COUNT, {
          menuItemStatus: "AVAILABLE",
          sort: "id,desc",
        });
        const res = await apiFetch<PaginatedResponse<MenuItemModel>>(
          `/menu-items/filters?${qs}`,
          { auth: false },
        );
        if (!cancelled) {
          setFeatured(res.data.content ?? []);
        }
      } catch (e) {
        if (!cancelled) {
          setFeatured([]);
          setFeaturedError(e instanceof ApiError ? e.message : "Không tải được món nổi bật");
        }
      } finally {
        if (!cancelled) setFeaturedLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <>
      {/* Explore More — tabs + lưới ảnh */}
      <section className="bg-background py-16 sm:py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <h2 className={sectionTitleClass} style={fontSerif}>
            Khám phá thêm
          </h2>

          <nav
            className="mt-8 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 sm:gap-x-10"
            aria-label="Danh mục khám phá"
          >
            {EXPLORE_TABS.map((tab) => {
              const active = tab === activeTab;
              return (
                <button
                  key={tab}
                  type="button"
                  onClick={() => setActiveTab(tab)}
                  className={`pb-1 text-sm font-medium transition sm:text-base ${
                    active
                      ? "border-b-2 border-brand-800 text-brand-800"
                      : "border-b-2 border-transparent text-muted hover:text-brand-800"
                  }`}
                >
                  {tab}
                </button>
              );
            })}
          </nav>
        </div>

        <ul className={EXPLORE_GRID}>
          {gallery.map((item, i) => (
            <li key={`${activeTab}-${i}`} className="min-w-0">
              <div
                className={
                  item.src
                    ? EXPLORE_IMAGE_FRAME
                    : `${EXPLORE_IMAGE_FRAME} bg-gradient-to-br ${item.tone ?? "from-brand-100 to-brand-800"}`
                }
              >
                <Image
                  src={item.src ?? "/logo2.jpg"}
                  alt={item.alt}
                  fill
                  className={
                    item.src ? EXPLORE_IMG : `${EXPLORE_IMG} opacity-90 mix-blend-overlay`
                  }
                  sizes={EXPLORE_IMAGE_SIZES}
                  priority={i < 3}
                />
              </div>
            </li>
          ))}
        </ul>
      </section>

      {/* About Us — banner tối */}
      <section id="about" className="bakery-about-bg relative scroll-mt-20 overflow-hidden py-20 sm:py-24">
        <div className="pointer-events-none absolute inset-0 bg-chocolate/20" aria-hidden />
        <div className="relative mx-auto max-w-2xl px-4 text-center sm:px-6">
          <h2 className="font-serif text-3xl font-semibold text-white sm:text-4xl" style={fontSerif}>
            Về chúng tôi
          </h2>
          <p className="mt-4 text-sm leading-relaxed text-brand-100 sm:text-base">
            Bistro mang đến không gian ấm áp, thực đơn theo mùa và dịch vụ đặt bàn — đặt món trực tuyến
            để bạn chỉ việc tận hưởng bữa ăn cùng người thân.
          </p>
          <Link href="/about" className={`${btnRectClass} mt-8`}>
            Tìm hiểu thêm
          </Link>
        </div>
      </section>

      {/* Featured Treats — lưới sản phẩm */}
      <section className="bg-background py-12 sm:py-16">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <h2 className={sectionTitleClass} style={fontSerif}>
            Món nổi bật
          </h2>

          {featuredError ? (
            <p className="mt-6 rounded-2xl bg-red-50 px-4 py-3 text-center text-sm text-red-800 ring-1 ring-red-100">
              {featuredError}
            </p>
          ) : null}

          {featuredLoading ? (
            <ul className={FEATURED_GRID} aria-busy="true">
              {Array.from({ length: FEATURED_COUNT }, (_, i) => (
                <li key={i}>
                  <div className={`${FEATURED_IMAGE_FRAME} animate-pulse bg-brand-100/40`} />
                </li>
              ))}
            </ul>
          ) : null}

          {!featuredLoading && !featuredError && featured.length === 0 ? (
            <p className="mt-6 text-center text-sm text-muted">Chưa có món AVAILABLE trong hệ thống.</p>
          ) : null}

          {!featuredLoading && featured.length > 0 ? (
            <ul className={FEATURED_GRID}>
              {featured.map((item) => (
                <li key={item.id}>
                  <div className={FEATURED_IMAGE_FRAME}>
                    {item.image?.startsWith("http") ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={item.image}
                        alt={item.name}
                        className={`h-full w-full ${FEATURED_IMG}`}
                      />
                    ) : (
                      <Image
                        src="/logo2.jpg"
                        alt={item.name}
                        fill
                        className={FEATURED_IMG}
                        sizes={FEATURED_IMAGE_SIZES}
                      />
                    )}
                  </div>
                </li>
              ))}
            </ul>
          ) : null}

          <div className="mt-8 text-center">
            <Link href="/menu" className={btnPrimaryClass}>
              Xem toàn bộ thực đơn
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
