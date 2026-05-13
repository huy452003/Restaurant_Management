export const MAX_PRICE_DIGITS = 9;
export const MAX_MENU_PRICE = 99_999_999.99;

export function formatVndThousands(digits: string): string {
  if (!digits) return "";
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

export function digitsOnlyFromPriceField(raw: string): string {
  return raw.replace(/\D/g, "").slice(0, MAX_PRICE_DIGITS);
}

export function parseMenuPriceFromDigits(digits: string): { ok: true; value: number } | { ok: false; message: string } {
  if (!digits) return { ok: false, message: "Nhập giá" };
  const n = Number(digits);
  if (!Number.isFinite(n) || n <= 0) return { ok: false, message: "Giá phải là số dương" };
  if (n > MAX_MENU_PRICE) return { ok: false, message: "Giá vượt quá giới hạn cho phép" };
  return { ok: true, value: n };
}

export function priceApiToDigits(price: string | undefined): string {
  if (!price) return "";
  const n = Number(price);
  if (!Number.isFinite(n) || n <= 0) return "";
  return String(Math.round(n));
}
