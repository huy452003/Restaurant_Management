/** Backend Jackson: @JsonFormat pattern "dd-MM-yyyy" */
export function formatBirthDdMmYyyy(isoDate: string): string {
  const d = new Date(isoDate + "T12:00:00");
  if (Number.isNaN(d.getTime())) return "";
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const yyyy = d.getFullYear();
  return `${dd}-${mm}-${yyyy}`;
}

/** dd-MM-yyyy → yyyy-MM-dd cho input type=date */
export function birthDdMmYyyyToInputDate(s: string | undefined): string {
  if (!s || !s.trim()) return "";
  const parts = s.trim().split("-");
  if (parts.length !== 3) return "";
  const [dd, mm, yyyy] = parts;
  if (!yyyy || !mm || !dd) return "";
  return `${yyyy.padStart(4, "0")}-${mm.padStart(2, "0")}-${dd.padStart(2, "0")}`;
}

/** Backend: "dd-MM-yyyy HH:mm:ss" — dùng giờ địa phương (không đổi UTC). */
export function formatLocalDateTimeDdMmYyyyHhMmSs(d: Date): string {
  if (Number.isNaN(d.getTime())) return "";
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const yyyy = d.getFullYear();
  const HH = String(d.getHours()).padStart(2, "0");
  const MM = String(d.getMinutes()).padStart(2, "0");
  const ss = String(d.getSeconds()).padStart(2, "0");
  return `${dd}-${mm}-${yyyy} ${HH}:${MM}:${ss}`;
}
