import type { ReactNode } from "react";

type Props = {
  children: ReactNode;
  wide?: boolean;
};

/** Form auth căn giữa trang, không panel ảnh (dùng cho account, v.v.) */
export function AuthCenteredLayout({ children, wide }: Props) {
  return (
    <div className="flex min-h-[calc(100dvh-4rem)] w-full items-center justify-center px-6 py-10 sm:px-10 lg:min-h-[calc(100dvh-8rem)]">
      <div className={`w-full ${wide ? "max-w-lg" : "max-w-md"}`}>
        <div className="rounded-2xl bg-white px-6 py-10 shadow-sm ring-1 ring-stone-200/80 sm:px-8">{children}</div>
      </div>
    </div>
  );
}
