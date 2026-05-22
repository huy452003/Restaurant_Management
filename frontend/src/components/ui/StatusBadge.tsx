"use client";

import {
  STATUS_BADGE_BASE,
  STATUS_BADGE_VARIANT_CLASS,
  statusBadgeClassName,
  statusBadgeLabel,
  type StatusBadgeDomain,
  type StatusBadgeVariant,
} from "@/lib/ui/status-badge";

type Props = {
  /** Nhãn tùy chỉnh; bỏ trống thì lấy theo domain + status. */
  label?: string;
  domain?: StatusBadgeDomain;
  status?: string;
  /** Ghi đè variant (bỏ qua domain/status). */
  variant?: StatusBadgeVariant;
  className?: string;
};

export function StatusBadge({ label, domain, status, variant, className }: Props) {
  const resolvedLabel =
    label ?? (domain ? statusBadgeLabel(domain, status) : "—");

  const cn =
    variant != null
      ? [STATUS_BADGE_BASE, STATUS_BADGE_VARIANT_CLASS[variant], className].filter(Boolean).join(" ")
      : domain
        ? statusBadgeClassName(domain, status, className)
        : [STATUS_BADGE_BASE, STATUS_BADGE_VARIANT_CLASS.neutral, className].filter(Boolean).join(" ");

  return <span className={cn}>{resolvedLabel}</span>;
}
