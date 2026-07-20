import type { LucideIcon } from "lucide-react";

/**
 * Shared empty-state treatment per the redesign spec: dashed border box + muted icon +
 * two-line copy. Used across every teacher list view (homework/submissions/exams/etc.)
 * instead of a bare "yo'q" table row.
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
}: {
  icon: LucideIcon;
  title: string;
  description?: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-border px-6 py-12 text-center">
      <Icon className="size-8 text-muted-foreground" strokeWidth={1.75} />
      <p className="text-sm font-medium">{title}</p>
      {description ? <p className="max-w-sm text-sm text-muted-foreground">{description}</p> : null}
    </div>
  );
}
