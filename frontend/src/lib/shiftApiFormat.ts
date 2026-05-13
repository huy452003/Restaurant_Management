export function formatShiftDateToApi(yyyyMmDd: string): string {
  const [y, m, d] = yyyyMmDd.split("-").map((x) => Number(x));
  if (!y || !m || !d) return yyyyMmDd;
  const dd = String(d).padStart(2, "0");
  const mm = String(m).padStart(2, "0");
  return `${dd}-${mm}-${y}`;
}

export function formatShiftDateTimeToApi(yyyyMmDd: string, hhMm: string): string {
  const [y, m, d] = yyyyMmDd.split("-").map((x) => Number(x));
  const [hh, mi] = hhMm.split(":").map((x) => Number(x));
  const dd = String(d).padStart(2, "0");
  const mm = String(m).padStart(2, "0");
  const HH = String(hh).padStart(2, "0");
  const MM = String(mi).padStart(2, "0");
  return `${dd}-${mm}-${y} ${HH}:${MM}:00`;
}

/** Chuẩn hóa về `YYYY-MM-DD` cho `<input type="date">`. */
export function parseApiShiftDateToInput(raw: string): string | null {
  const t = raw.trim();
  if (!t) return null;
  const iso = t.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (iso) return `${iso[1]}-${iso[2]}-${iso[3]}`;
  const dmy = t.match(/^(\d{2})-(\d{2})-(\d{4})/);
  if (dmy) return `${dmy[3]}-${dmy[2]}-${dmy[1]}`;
  return null;
}

/** Tách ngày + `HH:mm` cho input date/time từ chuỗi API (ISO hoặc dd-MM-yyyy HH:mm:ss). */
export function parseApiShiftDateTimeToInput(raw: string): { date: string; time: string } | null {
  const t = raw.trim();
  if (!t) return null;
  const iso = t.match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2})/);
  if (iso) return { date: `${iso[1]}-${iso[2]}-${iso[3]}`, time: `${iso[4]}:${iso[5]}` };
  const dmy = t.match(/^(\d{2})-(\d{2})-(\d{4})\s+(\d{2}):(\d{2})/);
  if (dmy) return { date: `${dmy[3]}-${dmy[2]}-${dmy[1]}`, time: `${dmy[4]}:${dmy[5]}` };
  return null;
}
