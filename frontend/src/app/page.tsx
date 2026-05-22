import Image from "next/image";
import { HomeLandingExplore } from "@/components/home/HomeLandingExplore";
import { HomeHeroActions } from "@/components/HomePageCtas";
import { fontSerif, sectionLabelClass } from "@/lib/ui/bakery";

export default function HomePage() {
  return (
    <main>
      <section className="bakery-hero bakery-pattern">
        <div className="bakery-hero-blob-a" aria-hidden />
        <div className="bakery-hero-blob-b" aria-hidden />
        <div className="bakery-hero-blob-c" aria-hidden />
        <div className="relative mx-auto flex max-w-6xl flex-col gap-12 px-4 py-16 sm:px-6 lg:flex-row lg:items-center lg:gap-16 lg:py-24">
          <div className="flex-1 space-y-6 lg:max-w-xl">
            <p className={sectionLabelClass}>Ẩm thực tươi mỗi ngày</p>
            <h1
              className="font-serif text-4xl font-semibold leading-[1.15] text-brand-900 sm:text-5xl lg:text-[3.25rem]"
              style={fontSerif}
            >
              Hương vị ấm áp
              <span className="mt-1 block text-brand-700">như ở nhà</span>
            </h1>
            <p className="max-w-md text-base leading-relaxed text-muted">
              Đặt món, đặt bàn và thanh toán trực tuyến — trải nghiệm nhà hàng hiện đại với không gian
              ấm cúng.
            </p>
            <HomeHeroActions />
          </div>
          <div className="flex flex-1 justify-center lg:justify-end">
            <div className="relative w-full max-w-md">
              <div
                className="absolute -inset-3 rounded-[2rem] bg-gradient-to-br from-blush via-brand-100 to-accent/30"
                aria-hidden
              />
              <div className="relative overflow-hidden rounded-[1.75rem] border border-brand-100 bg-surface p-3 shadow-[0_24px_48px_-12px_rgba(92,64,51,0.2)]">
                <div className="relative aspect-[4/5] w-full overflow-hidden rounded-[1.25rem] bg-brand-50">
                  <Image
                    src="/logoo1.png"
                    alt="Bistro"
                    fill
                    className="object-cover"
                    sizes="(max-width: 1024px) 100vw, 28rem"
                    priority
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <div className="mb-10 text-center">
          <p className={sectionLabelClass}>Dịch vụ</p>
          <h2
            className="mt-3 font-serif text-3xl font-semibold text-brand-900 sm:text-4xl"
            style={fontSerif}
          >
            Mọi thứ bạn cần
          </h2>
        </div>
        <div className="grid gap-8 md:grid-cols-3">
          {[
            {
              title: "Thực đơn tươi",
              desc: "Món được chế biến từ nguyên liệu chọn lọc, cập nhật theo mùa.",
              icon: "🍽️",
            },
            {
              title: "Đặt bàn nhanh",
              desc: "Chọn bàn, khung giờ và số khách — xác nhận chỉ vài bước.",
              icon: "📅",
            },
            {
              title: "Đơn & thanh toán",
              desc: "Thêm vào giỏ, thanh toán VNPay hoặc tại quầy khi đến.",
              icon: "✨",
            },
          ].map((c) => (
            <div
              key={c.title}
              className="group rounded-3xl border border-brand-100 bg-surface p-8 text-center shadow-[0_12px_40px_-16px_rgba(74,55,40,0.15)] transition hover:-translate-y-1 hover:shadow-[0_20px_48px_-16px_rgba(74,55,40,0.2)]"
            >
              <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-blush text-2xl">
                {c.icon}
              </span>
              <h3 className="mt-5 font-serif text-xl font-semibold text-brand-900" style={fontSerif}>
                {c.title}
              </h3>
              <p className="mt-2 text-sm leading-relaxed text-muted">{c.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <HomeLandingExplore />
    </main>
  );
}
