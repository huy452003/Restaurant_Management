/** Khớp yêu cầu nghiệp vụ; backend @PastOrPresent trên birth. */
export const MIN_USER_AGE_YEARS = 16;

export const MAX_USER_AGE_YEARS = 99;

export const BIRTH_AGE_RANGE_LABEL = `${MIN_USER_AGE_YEARS}–${MAX_USER_AGE_YEARS} tuổi`;

/** Lưu DD/MM/YYYY đang gõ khi chưa đủ — tránh reset ô input. */
export const BIRTH_PARTIAL_PREFIX = "P:";

export type BirthParts = { day: string; month: string; year: string };

export function emptyBirthParts(): BirthParts {
  return { day: "", month: "", year: "" };
}

function toIsoDateString(d: Date): string {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function startOfLocalDay(reference = new Date()): Date {
  return new Date(reference.getFullYear(), reference.getMonth(), reference.getDate(), 12, 0, 0);
}

export function sanitizeBirthPart(raw: string, maxLen: number): string {
  return raw.replace(/\D/g, "").slice(0, maxLen);
}

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

function parsePartNumber(value: string): number | null {
  if (!value.trim()) return null;
  const n = Number.parseInt(value, 10);
  return Number.isNaN(n) ? null : n;
}

function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

function maxDayInMonth(month: number, year: number | null): number {
  if (month === 2) {
    if (year !== null) return isLeapYear(year) ? 29 : 28;
    return 29;
  }
  if ([4, 6, 9, 11].includes(month)) return 30;
  return 31;
}

export function getBirthYearBounds(referenceDate = new Date()): { min: number; max: number } {
  const y = referenceDate.getFullYear();
  return { min: y - MAX_USER_AGE_YEARS, max: y - MIN_USER_AGE_YEARS };
}

/** Hiển thị không thêm số 0 đầu: `4` = ngày 4, `5` = tháng 5. */
function formatDayMonthDisplay(n: number): string {
  return String(n);
}

/** Chặn ngay khi gõ — không cho vượt 31 ngày / 12 tháng / dải năm 16–99 tuổi. */
export function constrainBirthDayInput(day: string, month: string, year: string): string {
  const d = sanitizeBirthPart(day, 2);
  if (!d) return "";

  let n = parsePartNumber(d);
  if (n === null) return "";

  if (d.length === 1) {
    if (n === 0) return "0";
    if (n > 9) return "9";
    return formatDayMonthDisplay(n);
  }

  if (n > 31) n = 31;
  if (n < 1) n = 1;

  const m = sanitizeBirthPart(month, 2);
  if (m.length >= 1) {
    const mn = parsePartNumber(m);
    if (mn !== null && mn >= 1 && mn <= 12) {
      const y = sanitizeBirthPart(year, 4);
      const yn = y.length === 4 ? parsePartNumber(y) : null;
      const cap = maxDayInMonth(mn, yn);
      if (n > cap) n = cap;
    }
  }

  return formatDayMonthDisplay(n);
}

export function constrainBirthMonthInput(month: string): string {
  const m = sanitizeBirthPart(month, 2);
  if (!m) return "";

  let n = parsePartNumber(m);
  if (n === null) return "";

  if (m.length === 1) {
    if (n === 0) return "0";
    if (n > 9) return "9";
    return formatDayMonthDisplay(n);
  }

  if (n > 12) n = 12;
  if (n < 1) n = 1;

  return formatDayMonthDisplay(n);
}

/** Tự sang ô tiếp theo: ngày 4–9 / tháng 2–9 chỉ cần một chữ số. */
export function shouldAutoAdvanceBirthPart(part: keyof BirthParts, value: string): boolean {
  const maxLen = part === "year" ? 4 : 2;
  const v = sanitizeBirthPart(value, maxLen);
  if (part === "year") return v.length >= 4;

  const n = parsePartNumber(v);
  if (n === null) return false;

  if (part === "day") {
    if (v.length >= 2) return true;
    return n >= 4 && n <= 9;
  }

  if (v.length >= 2) return true;
  return n >= 2 && n <= 9;
}

export function constrainBirthYearInput(year: string, referenceDate = new Date()): string {
  const y = sanitizeBirthPart(year, 4);
  if (y.length < 4) return y;

  const n = parsePartNumber(y);
  if (n === null) return y;

  const { min, max } = getBirthYearBounds(referenceDate);
  if (n > max) return String(max);
  if (n < min) return String(min);
  return y;
}

export function constrainBirthParts(parts: BirthParts, referenceDate = new Date()): BirthParts {
  const month = constrainBirthMonthInput(parts.month);
  const year = constrainBirthYearInput(parts.year, referenceDate);
  const day = constrainBirthDayInput(parts.day, month, year);
  return { day, month, year };
}

export function birthPartsToIso(day: string, month: string, year: string): string {
  const d = sanitizeBirthPart(day, 2);
  const m = sanitizeBirthPart(month, 2);
  const y = sanitizeBirthPart(year, 4);
  if (!d || !m || y.length < 4) return "";

  const dn = parsePartNumber(d);
  const mn = parsePartNumber(m);
  if (dn === null || mn === null || dn < 1 || dn > 31 || mn < 1 || mn > 12) return "";

  const iso = `${y}-${pad2(mn)}-${pad2(dn)}`;
  return parseIsoDateLocal(iso) ? iso : "";
}

export function serializeBirthField(parts: BirthParts): string {
  const iso = birthPartsToIso(parts.day, parts.month, parts.year);
  if (iso) return iso;
  if (!parts.day && !parts.month && !parts.year) return "";
  return `${BIRTH_PARTIAL_PREFIX}${parts.day}|${parts.month}|${parts.year}`;
}

export function deserializeBirthField(stored: string): BirthParts {
  if (!stored.trim()) return emptyBirthParts();
  if (stored.startsWith(BIRTH_PARTIAL_PREFIX)) {
    const [day = "", month = "", year = ""] = stored.slice(BIRTH_PARTIAL_PREFIX.length).split("|");
    return {
      day: sanitizeBirthPart(day, 2),
      month: sanitizeBirthPart(month, 2),
      year: sanitizeBirthPart(year, 4),
    };
  }
  return isoToBirthParts(stored);
}

export function isBirthFieldPartial(stored: string): boolean {
  return stored.startsWith(BIRTH_PARTIAL_PREFIX);
}

export function resolveBirthIsoFromField(stored: string): string {
  if (!stored.trim() || isBirthFieldPartial(stored)) return "";
  return stored.trim();
}

export function isoToBirthParts(iso: string): BirthParts {
  const parsed = parseIsoDateLocal(iso);
  if (!parsed) return emptyBirthParts();
  return {
    day: String(parsed.getDate()),
    month: String(parsed.getMonth() + 1),
    year: String(parsed.getFullYear()),
  };
}

export function parseIsoDateLocal(isoDate: string): Date | null {
  const trimmed = isoDate.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return null;
  const d = new Date(`${trimmed}T12:00:00`);
  if (Number.isNaN(d.getTime())) return null;
  if (toIsoDateString(d) !== trimmed) return null;
  return d;
}

/** Dán dd/mm/yyyy, dd-mm-yyyy hoặc 8 chữ số liền. */
export function parsePastedBirth(text: string): BirthParts | null {
  const trimmed = text.trim();
  if (!trimmed) return null;

  const eightDigits = trimmed.replace(/\D/g, "");
  if (eightDigits.length === 8) {
    return {
      day: eightDigits.slice(0, 2),
      month: eightDigits.slice(2, 4),
      year: eightDigits.slice(4, 8),
    };
  }

  const match = trimmed.match(/^(\d{1,2})[./-](\d{1,2})[./-](\d{4})$/);
  if (match) {
    return {
      day: sanitizeBirthPart(match[1], 2),
      month: sanitizeBirthPart(match[2], 2),
      year: sanitizeBirthPart(match[3], 4),
    };
  }

  return null;
}

function ageInFullYears(birth: Date, at: Date): number {
  let age = at.getFullYear() - birth.getFullYear();
  const monthDiff = at.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && at.getDate() < birth.getDate())) {
    age -= 1;
  }
  return age;
}

export const BIRTH_INVALID_MESSAGE = "Vui lòng nhập đúng ngày tháng năm sinh";

export const BIRTH_FUTURE_MESSAGE = "Định dạng ngày sinh sai";

export const BIRTH_DAY_INVALID_MESSAGE = "Ngày phải từ 01 đến 31";

export const BIRTH_MONTH_INVALID_MESSAGE = "Tháng phải từ 01 đến 12";

export const BIRTH_FEB_DAY_INVALID_MESSAGE = "Tháng 2 chỉ có tối đa 28 hoặc 29 ngày";

export const BIRTH_DAY_IN_MONTH_INVALID_MESSAGE = "Ngày không hợp lệ trong tháng đã chọn";

export const BIRTH_TOO_YOUNG_MESSAGE = `Bạn phải từ ${MIN_USER_AGE_YEARS} tuổi trở lên`;

export const BIRTH_TOO_OLD_MESSAGE = `Tuổi không được quá ${MAX_USER_AGE_YEARS}. Vui lòng nhập lại ngày sinh`;

/** Kiểm tra DD / MM — chấp nhận 1 chữ số (4 = ngày 4, 5 = tháng 5). */
export function validateBirthParts(parts: BirthParts): string | null {
  const day = sanitizeBirthPart(parts.day, 2);
  const month = sanitizeBirthPart(parts.month, 2);
  const year = sanitizeBirthPart(parts.year, 4);

  if (day.length >= 1) {
    const d = parsePartNumber(day);
    if (d === null || d < 1 || d > 31) return BIRTH_DAY_INVALID_MESSAGE;
  }

  if (month.length >= 1) {
    const m = parsePartNumber(month);
    if (m === null || m < 1 || m > 12) return BIRTH_MONTH_INVALID_MESSAGE;
  }

  if (day.length >= 1 && month.length >= 1) {
    const d = parsePartNumber(day)!;
    const m = parsePartNumber(month)!;

    if (m === 2 && d > 29) return BIRTH_FEB_DAY_INVALID_MESSAGE;

    const y = year.length === 4 ? parsePartNumber(year) : null;
    if (y !== null && (y < 1000 || y > 9999)) return BIRTH_INVALID_MESSAGE;

    const maxDay = maxDayInMonth(m, y);
    if (d > maxDay) {
      return m === 2 ? BIRTH_FEB_DAY_INVALID_MESSAGE : BIRTH_DAY_IN_MONTH_INVALID_MESSAGE;
    }
  }

  return null;
}

export function validateBirthIsoDate(isoDate: string, referenceDate = new Date()): string | null {
  if (!isoDate.trim()) return BIRTH_INVALID_MESSAGE;

  const parts = isoToBirthParts(isoDate);
  const partsError = validateBirthParts(parts);
  if (partsError) return partsError;

  const birth = parseIsoDateLocal(isoDate);
  if (!birth) return BIRTH_INVALID_MESSAGE;

  const today = startOfLocalDay(referenceDate);

  if (birth > today) return BIRTH_FUTURE_MESSAGE;

  const age = ageInFullYears(birth, today);
  if (age < MIN_USER_AGE_YEARS) return BIRTH_TOO_YOUNG_MESSAGE;
  if (age > MAX_USER_AGE_YEARS) return BIRTH_TOO_OLD_MESSAGE;

  return null;
}

/** Validate giá trị từ form (iso đủ hoặc chuỗi partial P:). */
export function validateBirthField(stored: string, referenceDate = new Date()): string | null {
  if (!stored.trim()) return BIRTH_INVALID_MESSAGE;

  const partsError = validateBirthParts(deserializeBirthField(stored));
  if (partsError) return partsError;

  const iso = resolveBirthIsoFromField(stored);
  if (!iso) return BIRTH_INVALID_MESSAGE;
  return validateBirthIsoDate(iso, referenceDate);
}

export function getBirthFieldError(
  stored: string,
  touched: boolean,
  referenceDate = new Date(),
): string | null {
  const partsError = validateBirthParts(deserializeBirthField(stored));
  if (partsError) return partsError;

  const iso = resolveBirthIsoFromField(stored);
  if (iso) return validateBirthIsoDate(iso, referenceDate);

  if (!touched) return null;
  if (!stored.trim()) return BIRTH_INVALID_MESSAGE;
  return BIRTH_INVALID_MESSAGE;
}
