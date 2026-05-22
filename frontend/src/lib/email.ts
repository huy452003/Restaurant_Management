/** Khớp backend: gmail.com, outlook.com, outlook.com.vn */
export const ALLOWED_USER_EMAIL_DOMAINS = ["gmail.com", "outlook.com", "outlook.com.vn"] as const;

export const USER_EMAIL_DOMAIN_MESSAGE =
  "Chỉ chấp nhận email @gmail.com, @outlook.com hoặc @outlook.com.vn";

export function getEmailDomain(email: string): string | null {
  const at = email.lastIndexOf("@");
  if (at < 0 || at === email.length - 1) return null;
  return email.slice(at + 1).trim().toLowerCase();
}

export function isAllowedUserEmail(email: string): boolean {
  const domain = getEmailDomain(email);
  if (!domain) return false;
  return (ALLOWED_USER_EMAIL_DOMAINS as readonly string[]).includes(domain);
}
