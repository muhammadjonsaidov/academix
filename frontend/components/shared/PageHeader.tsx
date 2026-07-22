import { cn } from "@/lib/utils";

interface PageHeaderProps {
  title: React.ReactNode;
  description: string;
  className?: string;
}

/**
 * Shared dashboard page-header block: heading-face title, a short role-accent
 * rule (the ONE role-atmosphere element in the header — picks up its hue from
 * the DashboardShell's data-role via var(--accent-role)), and a one-line
 * muted description. Used by every role's index page so the five dashboards
 * open with the same typographic rhythm.
 */
export function PageHeader({ title, description, className }: PageHeaderProps) {
  return (
    <div className={cn("space-y-1.5", className)}>
      <h2 className="font-heading text-xl font-semibold">{title}</h2>
      <div className="bg-accent-role h-0.5 w-8 rounded-full" aria-hidden="true" />
      <p className="text-sm text-muted-foreground">{description}</p>
    </div>
  );
}
