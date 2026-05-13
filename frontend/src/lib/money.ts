export function formatVnd(amount: string | number | undefined | null): string {
  if (amount === undefined || amount === null || amount === "") return "—";
  const n = typeof amount === "string" ? Number(amount) : amount;
  if (Number.isNaN(n)) return String(amount);
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(n);
}
