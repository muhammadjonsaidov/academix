import { cn } from "@/lib/utils";

interface PageHeaderProps {
  title: React.ReactNode;
  description: React.ReactNode;
  /** @deprecated No longer rendered — labels above headings are noise (docs/design.md). */
  eyebrow?: string;
  actions?: React.ReactNode;
  className?: string;
}

/** The page's h1, one plain sentence under it, and the page's actions on the right. */
export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
  return (
    <div className={cn("flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between", className)}>
      <div className="space-y-1.5">
        <h1 className="font-heading text-2xl font-semibold tracking-tight">{title}</h1>
        <p className="max-w-2xl text-[0.9375rem] text-muted-foreground">{description}</p>
      </div>
      {actions ? <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  );
}
