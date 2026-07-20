import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  className?: string;
}

/** Local copy of the shared empty-state pattern (dashed border + muted icon + two-line
 * copy) — kept per-role since `components/admin/EmptyState.tsx` isn't a cross-role shared
 * component yet (see `components/shared/` for genuinely shared pieces). */
export function EmptyState({ icon: Icon, title, description, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border px-6 py-10 text-center",
        className,
      )}
    >
      <Icon className="size-10 text-muted-foreground/40" strokeWidth={1.75} />
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="max-w-xs text-sm text-muted-foreground">{description}</p>
    </div>
  );
}
