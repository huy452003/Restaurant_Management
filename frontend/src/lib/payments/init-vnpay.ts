import { apiFetch, ApiError } from "@/lib/api/client";
import type { VnpayCheckoutResponse } from "@/lib/api/types";
import { openUrlInNewTab } from "@/lib/payments/open-url-in-new-tab";

export async function initVnpayCheckout(orderNumber: string): Promise<{ ok: true } | { ok: false; message: string }> {
  try {
    const res = await apiFetch<VnpayCheckoutResponse>("/payments/vnpay/init", {
      method: "POST",
      body: JSON.stringify({ orderNumber }),
    });
    const url = res.data?.paymentUrl?.trim();
    if (!url) {
      return { ok: false, message: "Không nhận được link thanh toán VNPAY" };
    }
    const opened = openUrlInNewTab(url);
    if (!opened) {
      return {
        ok: false,
        message: "Trình duyệt chặn cửa sổ mới. Cho phép popup cho trang này rồi thử lại.",
      };
    }
    return { ok: true };
  } catch (e) {
    return { ok: false, message: e instanceof ApiError ? e.message : "Khởi tạo VNPAY thất bại" };
  }
}
