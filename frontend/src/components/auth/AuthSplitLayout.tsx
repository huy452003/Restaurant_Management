import Image from "next/image";
import type { ReactNode } from "react";

type Props = {
  children: ReactNode;
  wide?: boolean;
};

/** Split: trái ảnh nền kem (bakery), phải form trắng */
export function AuthSplitLayout({ children, wide }: Props) {
  return (
    <div className="flex min-h-[calc(100dvh-4rem)] w-full flex-col lg:min-h-[calc(100dvh-8rem)] lg:flex-row">
      <aside className="bakery-hero bakery-pattern relative flex min-h-56 w-full shrink-0 items-center justify-center overflow-hidden lg:min-h-full lg:w-1/2 lg:max-w-[50%]">
        <div className="bakery-hero-blob-a" aria-hidden />
        <div className="bakery-hero-blob-b scale-75" aria-hidden />
        <div className="relative z-10 flex h-full w-full items-center justify-center px-6 py-10 sm:px-10 lg:px-12 lg:py-14 xl:px-16">
          <div className="relative w-full max-w-[280px] sm:max-w-xs lg:max-w-sm xl:max-w-md">
            <div
              className="absolute -inset-2 rounded-[1.75rem] bg-gradient-to-br from-blush to-brand-100"
              aria-hidden
            />
            <div className="relative overflow-hidden rounded-[1.5rem] border border-brand-100 bg-surface p-2 shadow-[0_20px_40px_-12px_rgba(92,64,51,0.18)]">
              <div className="relative aspect-[4/5] w-full overflow-hidden rounded-[1.15rem] bg-brand-50 sm:aspect-[3/4] lg:aspect-[4/5] lg:min-h-[min(72vh,520px)]">
                <Image
                  src="/logo2.jpg"
                  alt="Bistro"
                  fill
                  priority
                  className="object-cover"
                  sizes="(max-width: 1024px) 85vw, 42vw"
                />
              </div>
            </div>
          </div>
        </div>
      </aside>

      <div className="flex flex-1 flex-col justify-center bg-surface px-6 py-10 sm:px-10 lg:w-1/2 lg:px-14 lg:py-14 xl:px-20">
        <div className={`mx-auto w-full ${wide ? "max-w-lg" : "max-w-md"}`}>{children}</div>
      </div>
    </div>
  );
}
