/** Khớp backend: 11 số, 84 + đầu số di động VN (3, 5, 7, 8, 9). */
export const VIETNAM_MOBILE_11_PATTERN = /^84[35789][0-9]{8}$/;

const MOBILE_PREFIX = "[35789]";

/** 9 số: 9xxxxxxxx hoặc 10 số: 09xxxxxxxx */
const PHONE_LOCAL_PATTERN = new RegExp(
  `^(?:${MOBILE_PREFIX}[0-9]{8}|0${MOBILE_PREFIX}[0-9]{8})$`,
);

export const PHONE_LOCAL_MAX_LENGTH = 10;

export const PHONE_VALIDATION_MESSAGE =
  "Nhập 9 số (9xxxxxxxx) hoặc 10 số bắt đầu 0 (09xxxxxxxx)";

export function digitsOnlyPhone(value: string): string {
  return value.replace(/\D/g, "");
}

/** Phần số sau +84 để hiển thị trong ô nhập. */
export function phoneToLocalDisplay(stored: string): string {
  const digits = digitsOnlyPhone(stored);
  if (digits.startsWith("84") && digits.length > 2) {
    return digits.slice(2);
  }
  return digits;
}

/** Chỉ giữ 9–10 chữ số phần quốc gia (không gõ 84). */
export function sanitizePhoneLocalInput(value: string): string {
  let digits = digitsOnlyPhone(value);
  if (digits.startsWith("84")) {
    digits = digits.slice(2);
  }
  return digits.slice(0, PHONE_LOCAL_MAX_LENGTH);
}

/** Gửi API / lưu DB: 84 + 9 số quốc gia. */
export function normalizeVietnamMobilePhone(phoneOrLocal: string): string {
  const digits = digitsOnlyPhone(phoneOrLocal);
  if (digits.startsWith("84") && digits.length === 11) {
    return digits;
  }
  if (digits.startsWith("84") && digits.length > 2) {
    return "84" + digits.slice(2, 11);
  }
  if (digits.length === 10 && digits.startsWith("0")) {
    return "84" + digits.slice(1);
  }
  if (digits.length === 9) {
    return "84" + digits;
  }
  return digits;
}

export function isValidPhoneLocal(local: string): boolean {
  return PHONE_LOCAL_PATTERN.test(sanitizePhoneLocalInput(local));
}

export function isValidPhoneDigits(phoneOrLocal: string): boolean {
  return VIETNAM_MOBILE_11_PATTERN.test(normalizeVietnamMobilePhone(phoneOrLocal));
}

/** @deprecated Dùng sanitizePhoneLocalInput — giữ cho chỗ chưa đổi component. */
export function sanitizePhoneDigits(value: string): string {
  return sanitizePhoneLocalInput(value);
}
