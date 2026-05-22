import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { AboutPageCtas } from "@/components/about/AboutPageCtas";
import {
  btnPrimaryClass,
  btnSecondaryClass,
  cardClass,
  fontSerif,
  sectionLabelClass,
} from "@/lib/ui/bakery";

export const metadata: Metadata = {
  title: "Về chúng tôi — Bistro",
  description:
    "Khám phá câu chuyện Bistro — ẩm thực theo mùa, không gian ấm cúng và dịch vụ đặt bàn trực tuyến tại TP. Hồ Chí Minh.",
};

const VALUES = [
  {
    title: "Nguyên liệu chọn lọc",
    desc: "Ưu tiên nguồn cung địa phương, theo mùa vụ và minh bạch về xuất xứ.",
    icon: "🌿",
  },
  {
    title: "Chế biến tận tâm",
    desc: "Mỗi món được hoàn thiện tại bếp mở — cân bằng hương vị truyền thống và hiện đại.",
    icon: "👨‍🍳",
  },
  {
    title: "Không gian ấm cúng",
    desc: "Thiết kế dễ chịu cho bữa gia đình, hẹn hò hay tiếp khách đối tác.",
    icon: "✨",
  },
  {
    title: "Phục vụ linh hoạt",
    desc: "Đặt bàn, gọi món và thanh toán trực tuyến — tiết kiệm thời gian cho bạn.",
    icon: "📱",
  },
] as const;

const HOURS = [
  { day: "Thứ Hai — Thứ Sáu", time: "10:00 — 22:00" },
  { day: "Thứ Bảy — Chủ Nhật", time: "09:00 — 23:00" },
] as const;

export default function AboutPage() {
  return (
    <main>
      {/* Hero */}
      <section className="bakery-hero bakery-pattern">
        <div className="bakery-hero-blob-a" aria-hidden />
        <div className="bakery-hero-blob-b" aria-hidden />
        <div className="relative mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-24">
          <div className="mx-auto max-w-3xl text-center">
            <p className={sectionLabelClass}>Câu chuyện của chúng tôi</p>
            <h1
              className="mt-5 font-serif text-4xl font-semibold leading-tight text-brand-900 sm:text-5xl"
              style={fontSerif}
            >
              Bistro — nơi ẩm thực gặp gỡ sự chân thành
            </h1>
            <p className="mx-auto mt-5 max-w-2xl text-base leading-relaxed text-muted">
              Từ một căn bếp nhỏ đầy đam mê, chúng tôi trở thành điểm hẹn quen thuộc của những ai yêu thích
              bữa ăn có chiều sâu, phục vụ tử tế và không gian như ở nhà.
            </p>
            <AboutPageCtas className="mt-8 justify-center" />
          </div>
        </div>
      </section>

      {/* Câu chuyện */}
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="grid items-center gap-12 lg:grid-cols-2 lg:gap-16">
          <div className="relative order-2 lg:order-1">
            <div
              className="absolute -inset-3 rounded-[2rem] bg-gradient-to-br from-blush via-brand-100 to-accent/25"
              aria-hidden
            />
            <div className="relative overflow-hidden rounded-[1.5rem] border border-brand-100 shadow-[0_20px_48px_-16px_rgba(74,55,40,0.18)]">
              <div className="relative aspect-[4/3] w-full">
                <Image
                  src="/logo2.jpg"
                  alt="Không gian Bistro"
                  fill
                  className="object-cover"
                  sizes="(max-width: 1024px) 100vw, 50vw"
                />
              </div>
            </div>
          </div>
          <div className="order-1 space-y-5 lg:order-2">
            <p className={sectionLabelClass}>Hành trình</p>
            <h2 className="font-serif text-3xl font-semibold text-brand-900 sm:text-4xl" style={fontSerif}>
              Bắt đầu từ niềm tin vào bữa ăn có ý nghĩa
            </h2>
            <p className="text-sm leading-relaxed text-muted sm:text-base">
              Bistro ra đời với mong muốn mang đến trải nghiệm nhà hàng bình dân nhưng chỉn chu: thực đơn
              thay đổi theo mùa, công thức được trau chuốt qua nhiều năm và đội ngũ luôn lắng nghe phản hồi
              của khách.
            </p>
            <p className="text-sm leading-relaxed text-muted sm:text-base">
              Chúng tôi tin rằng một bữa ăn tốt không chỉ là món ngon — mà còn là sự chu đáo trong từng
              chi tiết: từ ánh sáng trong phòng ăn, nhạc nền nhẹ nhàng, đến cách nhân viên gợi ý món phù hợp
              khẩu vị của bạn.
            </p>
            <p className="text-sm leading-relaxed text-muted sm:text-base">
              Hôm nay, Bistro phục vụ cả khách địa phương lẫn du khách, với dịch vụ đặt bàn và đặt món trực
              tuyến giúp bạn sắp xếp buổi sum họp, tiệc sinh nhật hay bữa tối lãng mạn chỉ trong vài phút.
            </p>
          </div>
        </div>
      </section>

      {/* Giá trị */}
      <section className="border-y border-brand-100 bg-brand-50/40 py-16 sm:py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="mb-10 text-center">
            <p className={sectionLabelClass}>Triết lý</p>
            <h2 className="mt-3 font-serif text-3xl font-semibold text-brand-900 sm:text-4xl" style={fontSerif}>
              Điều chúng tôi theo đuổi mỗi ngày
            </h2>
          </div>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {VALUES.map((v) => (
              <div key={v.title} className={`${cardClass} p-6 text-center`}>
                <span className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-blush text-xl">
                  {v.icon}
                </span>
                <h3 className="mt-4 font-serif text-lg font-semibold text-brand-900" style={fontSerif}>
                  {v.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-muted">{v.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Trải nghiệm tại nhà hàng */}
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="grid gap-10 lg:grid-cols-2">
          <div className={`${cardClass} p-8`}>
            <h2 className="font-serif text-2xl font-semibold text-brand-900" style={fontSerif}>
              Dùng bữa tại chỗ
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-muted">
              Không gian chính với bàn hai và bốn người, góc riêng cho nhóm nhỏ. Nhân viên hỗ trợ gợi ý
              món kết hợp rượu vang hoặc đồ uống không cồn. Vui lòng đặt bàn trước vào cuối tuần và dịp lễ.
            </p>
            <ul className="mt-4 space-y-2 text-sm text-brand-800">
              <li>• Thực đơn theo mùa, cập nhật định kỳ</li>
              <li>• Set menu cho tiệc công ty (từ 8 khách)</li>
              <li>• Góc trẻ em thân thiện vào cuối tuần</li>
            </ul>
          </div>
          <div className={`${cardClass} p-8`}>
            <h2 className="font-serif text-2xl font-semibold text-brand-900" style={fontSerif}>
              Đặt món & thanh toán online
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-muted">
              Đăng ký tài khoản để xem thực đơn, thêm món vào giỏ và thanh toán VNPay — tiện cho bữa trưa
              văn phòng hoặc mang về. Đơn dine-in tại bàn vẫn được phục vụ bởi đội ngũ saler.
            </p>
            <ul className="mt-4 space-y-2 text-sm text-brand-800">
              <li>• Theo dõi trạng thái đơn trên ứng dụng</li>
              <li>• Đặt bàn trực tuyến, chọn khung giờ 30 phút</li>
              <li>• Hỗ trợ yêu cầu đặc biệt qua ghi chú đặt chỗ</li>
            </ul>
          </div>
        </div>
      </section>

      {/* Đầu bếp / đội ngũ */}
      <section className="bakery-about-bg relative overflow-hidden py-16 sm:py-20">
        <div className="pointer-events-none absolute inset-0 bg-chocolate/25" aria-hidden />
        <div className="relative mx-auto max-w-3xl px-4 text-center sm:px-6">
          <p className="text-xs font-semibold uppercase tracking-wider text-accent">Đội ngũ bếp</p>
          <h2 className="mt-3 font-serif text-3xl font-semibold text-white sm:text-4xl" style={fontSerif}>
            Tâm huyết trong từng món ăn
          </h2>
          <p className="mt-4 text-sm leading-relaxed text-brand-100 sm:text-base">
            Đầu bếp trưởng và đội bếp của Bistro có kinh nghiệm tại các nhà hàng Âu — Á, luôn thử nghiệm
            công thức mới nhưng giữ trọn vẹn hồn cốt món quen thuộc của khách Việt. Mỗi tuần, bếp trưởng
            chọn nguyên liệu đặc sắc từ nông trại đối tác để đưa vào thực đơn cuối tuần.
          </p>
        </div>
      </section>

      {/* Giờ mở cửa & liên hệ */}
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-20">
        <div className="grid gap-10 lg:grid-cols-2">
          <div>
            <p className={sectionLabelClass}>Ghé thăm</p>
            <h2 className="mt-3 font-serif text-3xl font-semibold text-brand-900" style={fontSerif}>
              Giờ mở cửa & liên hệ
            </h2>
            <ul className="mt-6 space-y-4">
              {HOURS.map((row) => (
                <li
                  key={row.day}
                  className="flex flex-wrap items-baseline justify-between gap-2 border-b border-brand-100 pb-3 last:border-0"
                >
                  <span className="font-medium text-brand-900">{row.day}</span>
                  <span className="text-muted">{row.time}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className={`${cardClass} p-8`}>
            <h3 className="font-serif text-xl font-semibold text-brand-900" style={fontSerif}>
              Thông tin liên hệ
            </h3>
            <ul className="mt-5 space-y-3 text-sm text-muted">
              <li>
                <span className="font-medium text-brand-800">Địa chỉ:</span>
                <br />
                123 Đường Ẩm Thực, Quận 1, TP. Hồ Chí Minh
              </li>
              <li>
                <span className="font-medium text-brand-800">Điện thoại:</span>{" "}
                <a href="tel:19000000" className="text-brand-700 hover:underline">
                  1900 0000
                </a>
              </li>
              <li>
                <span className="font-medium text-brand-800">Email:</span>{" "}
                <a href="mailto:huyk3@bistro.vn" className="text-brand-700 hover:underline">
                  huyk3@bistro.vn
                </a>
              </li>
            </ul>
            <p className="mt-6 text-xs text-muted">
              Bãi đỗ xe máy miễn phí. Gửi xe ô tô hỗ trợ trong vòng 50m (có phí theo giờ).
            </p>
          </div>
        </div>
      </section>

      {/* CTA cuối */}
      <section className="border-t border-brand-100 bg-gradient-to-b from-blush/30 to-cream py-16 sm:py-20">
        <div className="mx-auto max-w-2xl px-4 text-center sm:px-6">
          <h2 className="font-serif text-3xl font-semibold text-brand-900 sm:text-4xl" style={fontSerif}>
            Hẹn gặp bạn tại Bistro
          </h2>
          <p className="mt-3 text-sm text-muted sm:text-base">
            Đặt bàn trước để giữ chỗ đẹp, hoặc khám phá thực đơn và đặt món ngay hôm nay.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link href="/reservations" className={btnPrimaryClass}>
              Đặt bàn
            </Link>
            <Link href="/menu" className={btnSecondaryClass}>
              Xem thực đơn
            </Link>
            <Link href="/" className={btnSecondaryClass}>
              Về trang chủ
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}
