import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: ReactNode;
  className?: string;
}

/** Shared empty-state box per the design system: dashed border + muted icon + two-line
 * copy, never a bare gray paragraph. */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border px-6 py-10 text-center",
        className,
      )}
    >
      <span className="flex size-14 items-center justify-center rounded-full bg-muted/60">
        <Icon className="size-6 text-muted-foreground/60" strokeWidth={1.75} />
      </span>
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="max-w-xs text-sm text-muted-foreground">{description}</p>
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}
