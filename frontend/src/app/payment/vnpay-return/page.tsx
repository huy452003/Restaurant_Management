"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect } from "react";

const VNP_SUCCESS = "00";

function VnpayReturnContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const rsp = searchParams.get("vnp_ResponseCode") ?? "";
  const audience = searchParams.get("audience");
  const orderNumber = searchParams.get("orderNumber")?.trim() ?? "";

  const target =
    audience === "staff"
      ? "/staff/orders"
      : "/orders";

  useEffect(() => {
    const params = new URLSearchParams();
    if (rsp === VNP_SUCCESS) {
      params.set("payment", "success");
    } else {
      params.set("payment", "cancelled");
    }
    if (orderNumber) {
      params.set("orderNumber", orderNumber);
    }
    const qs = params.toString();
    const id = window.setTimeout(() => {
      router.replace(qs ? `${target}?${qs}` : target);
    }, 1200);
    return () => window.clearTimeout(id);
  }, [router, rsp, target, orderNumber]);

  const cancelled = rsp !== VNP_SUCCESS;
  const message = cancelled
    ? "Bạn đã hủy thanh toán. Đang chuyển về trang đơn hàng…"
    : "Thanh toán thành công. Đang chuyển về trang đơn hàng…";

  return (
    <div className="mx-auto flex min-h-[50vh] max-w-md flex-col items-center justify-center px-4 text-center">
      <p className={`text-sm ${cancelled ? "text-red-700" : "text-emerald-800"}`}>{message}</p>
    </div>
  );
}

export default function VnpayReturnPage() {
  return (
    <Suspense fallback={<div className="py-20 text-center text-muted">Đang xử lý…</div>}>
      <VnpayReturnContent />
    </Suspense>
  );
}
