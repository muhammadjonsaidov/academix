import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  className?: string;
}

/** Shared empty-state box per the design system: dashed border + muted icon + two-line
 * copy, never a bare gray paragraph. Mirrors components/admin/EmptyState.tsx (kept
 * per-role rather than imported cross-scope). */
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
