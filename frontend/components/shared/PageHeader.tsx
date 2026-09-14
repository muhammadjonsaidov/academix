import { cn } from "@/lib/utils";

interface PageHeaderProps {
  title: React.ReactNode;
  description: React.ReactNode;
  /** Short uppercase eyebrow above the title (e.g. "Panel" or a section name).
   * Wears the dashboard's role accent via the data-role layer. */
  eyebrow?: string;
  actions?: React.ReactNode;
  className?: string;
}

/**
 * Shared dashboard page-header block: role-accent eyebrow + heading-face title with
 * a gradient role-accent rule, and a one-line muted description. It owns the route's
 * semantic h1, while the app frame only provides user/session context.
 */
export function PageHeader({
  title,
  description,
  eyebrow = "Panel",
  actions,
  className,
}: PageHeaderProps) {
  return (
    <div className={cn("flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between", className)}>
      <div className="space-y-2">
        <div className="flex items-center gap-2.5">
          <span className="bg-accent-role h-1 w-6 rounded-full" aria-hidden="true" />
          <span className="text-accent-role text-xs font-semibold tracking-[0.16em] uppercase">
            {eyebrow}
          </span>
        </div>
        <h1 className="font-heading text-2xl font-semibold tracking-tight">{title}</h1>
        <div
          className="bg-accent-role h-0.5 w-16 rounded-full opacity-60"
          aria-hidden="true"
        />
        <p className="max-w-3xl text-sm text-muted-foreground">{description}</p>
      </div>
      {actions ? <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}
