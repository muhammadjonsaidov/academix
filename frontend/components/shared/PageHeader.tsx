import { cn } from "@/lib/utils";

interface PageHeaderProps {
  title: React.ReactNode;
  description: string;
  /** Short uppercase eyebrow above the title (e.g. "Panel" or a section name).
   * Wears the dashboard's role accent via the data-role layer. */
  eyebrow?: string;
  className?: string;
}

/**
 * Shared dashboard page-header block: role-accent eyebrow + heading-face title with
 * a gradient role-accent rule, and a one-line muted description. Used by every
 * role's index page so the five dashboards open with the same typographic rhythm.
 */
export function PageHeader({ title, description, eyebrow = "Panel", className }: PageHeaderProps) {
  return (
    <div className={cn("space-y-2", className)}>
      <div className="flex items-center gap-2.5">
        <span className="bg-accent-role h-1 w-6 rounded-full" aria-hidden="true" />
        <span className="text-accent-role text-xs font-semibold tracking-[0.16em] uppercase">
          {eyebrow}
        </span>
      </div>
      <h2 className="font-heading text-2xl font-semibold tracking-tight">{title}</h2>
      <div
        className="bg-accent-role h-0.5 w-16 rounded-full opacity-60"
        aria-hidden="true"
      />
      <p className="text-sm text-muted-foreground">{description}</p>
    </div>
  );
}
