import Image from "next/image";
import { HomeBottomCta, HomeHeroActions } from "@/components/HomePageCtas";

export default function HomePage() {
  return (
    <main>
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-900 via-brand-800 to-stone-900 text-white">
        <div
          className="pointer-events-none absolute inset-0 opacity-30"
          style={{
            backgroundImage:
              "url(\"data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.06'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E\")",
          }}
        />
        <div className="relative mx-auto flex max-w-6xl flex-col gap-10 px-4 py-20 sm:px-6 lg:flex-row lg:items-center lg:py-28">
          <div className="flex-1 space-y-6">
            <p className="inline-flex rounded-full bg-white/10 px-4 py-1 text-sm font-medium text-amber-100/90 ring-1 ring-white/20">
              Ẩm thực &amp; không gian ấm cúng
            </p>
            <h1
              className="font-serif text-4xl font-semibold leading-tight sm:text-5xl lg:text-6xl"
              style={{ fontFamily: "var(--font-cormorant), serif" }}
            >
              Trải nghiệm bữa tối
              <span className="block text-amber-200/95">như ở nhà</span>
            </h1>
            <HomeHeroActions />
          </div>
          <div className="flex flex-1 justify-center lg:justify-end">
            <div className="relative w-full max-w-md rounded-2xl border border-white/10 bg-white/5 p-6 shadow-2xl backdrop-blur-md">
              <div className="relative aspect-[4/3] w-full overflow-hidden rounded-xl bg-stone-900/50 ring-1 ring-white/10">
                <Image
                  src="/logo1.png"
                  alt="Bistro"
                  fill
                  className="object-contain p-4"
                  sizes="(max-width: 1024px) 100vw, 28rem"
                  priority
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <div className="grid gap-10 md:grid-cols-3">
          {[
            {
              title: "Thực đơn tươi tác",
              desc: "Luôn mang đến hương vị tốt nhất cho khách hàng.",
            },
            {
              title: "Đặt bàn nhanh",
              desc: "Khách hàng có thể đặt bàn trực tiếp qua hệ thống đặt bàn của chúng tôi.",
            },
            {
              title: "Đơn & thanh toán",
              desc: "Tạo đơn và thanh toán trực tiếp qua hệ thống thanh toán của chúng tôi.",
            },
          ].map((c) => (
            <div
              key={c.title}
              className="rounded-2xl border border-stone-200 bg-surface p-6 shadow-sm transition hover:shadow-md"
            >
              <div className="mb-4 h-1 w-12 rounded-full bg-accent" />
              <h2 className="font-serif text-xl font-semibold text-brand-900" style={{ fontFamily: "var(--font-cormorant), serif" }}>
                {c.title}
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-muted">{c.desc}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="border-y border-stone-200 bg-brand-50/60">
        <HomeBottomCta />
      </section>
    </main>
  );
}
