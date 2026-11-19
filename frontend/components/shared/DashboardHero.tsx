import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface DashboardHeroProps {
  title: React.ReactNode;
  description: React.ReactNode;
  /** @deprecated No longer rendered (docs/design.md: no labels above headings). */
  eyebrow?: string;
  /** Right-side cluster — HeroAction shortcuts, or nothing. */
  actions?: React.ReactNode;
  className?: string;
}

/**
 * Opening of every role's home page: a greeting, one sentence that says what needs doing, and
 * shortcuts. Deliberately plain — the page's own content (the review queue, the child's grades)
 * is the hero, not a banner.
 */
export function DashboardHero({ title, description, actions, className }: DashboardHeroProps) {
  return (
    <section
      className={cn(
        "flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between lg:gap-6",
        className,
      )}
    >
      <div className="min-w-0 space-y-1.5">
        <h1 className="font-heading text-[1.75rem] font-bold tracking-tight sm:text-3xl">{title}</h1>
        <p className="max-w-2xl text-[0.9375rem] text-muted-foreground">{description}</p>
      </div>
      {actions ? <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div> : null}
    </section>
  );
}

interface HeroActionProps {
  href: string;
  icon: LucideIcon;
  children: React.ReactNode;
}

/**
 * Quick-action button for the hero — outline with this role's accent as the
 * active color (secondary tier). Primary buttons stay ink everywhere per the
 * design system; these are role-tinted navigation shortcuts, hence accent.
 */
export function HeroAction({ href, icon: Icon, children }: HeroActionProps) {
  return (
    <Button
      variant="outline"
      size="sm"
      render={<Link href={href} />}
    >
      <Icon className="size-3.5" strokeWidth={1.75} />
      {children}
    </Button>
  );
}
