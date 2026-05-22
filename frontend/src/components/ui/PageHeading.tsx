import type { ReactNode } from "react";
import { fontSerif, pageSubtitleClass, pageTitleClass } from "@/lib/ui/bakery";

type Props = {
  title: string;
  subtitle?: string;
  action?: ReactNode;
};

export function PageHeading({ title, subtitle, action }: Props) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className={pageTitleClass} style={fontSerif}>
          {title}
        </h1>
        {subtitle ? <p className={pageSubtitleClass}>{subtitle}</p> : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </div>
  );
}
