import Link from "next/link";
import type { LucideIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface DashboardHeroProps {
  title: React.ReactNode;
  description: React.ReactNode;
  /** Short uppercase eyebrow above the title — wears the role accent. */
  eyebrow?: string;
  /** Right-side cluster — HeroAction quick buttons, info chips, or nothing. */
  actions?: React.ReactNode;
  className?: string;
}

/**
 * Role dashboard hero — the personalized opening band of every role's index page.
 * Wears the shell's role accent (resolves via the data-role layer in globals.css)
 * through ambient corner washes, an accent eyebrow and accent-styled quick actions;
 * the body itself stays paper/ink so the whole thing reads as one calm sheet with a
 * color note, not a marketing banner. Renders fine before dashboard data loads —
 * pages pass a fallback description until their store resolves.
 */
export function DashboardHero({
  title,
  description,
  eyebrow = "Bosh sahifa",
  actions,
  className,
}: DashboardHeroProps) {
  return (
    <section
      className={cn(
        "hero-sweep relative overflow-hidden rounded-xl border border-border bg-card",
        className,
      )}
    >
      {/* Role-accent atmosphere — atmosphere only, behind the content */}
      <span
        aria-hidden
        className="bg-accent-role/10 pointer-events-none absolute -top-24 right-0 size-72 rounded-full blur-3xl"
      />
      <span
        aria-hidden
        className="bg-accent-role/8 pointer-events-none absolute -bottom-24 -left-16 size-64 rounded-full blur-3xl"
      />
      <div className="relative flex flex-col gap-4 p-5 sm:p-6 lg:flex-row lg:items-end lg:justify-between lg:gap-6">
        <div className="min-w-0 space-y-2">
          <div className="flex items-center gap-2.5">
            <span className="bg-accent-role h-1 w-6 rounded-full" aria-hidden="true" />
            <span className="text-accent-role text-xs font-semibold tracking-[0.16em] uppercase">
              {eyebrow}
            </span>
          </div>
          <h1 className="font-heading text-2xl font-semibold tracking-tight sm:text-[1.7rem]">{title}</h1>
          <p className="max-w-xl text-sm text-muted-foreground">{description}</p>
        </div>
        {actions ? (
          <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>
        ) : null}
      </div>
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
      className="text-accent-role hover:border-accent-role/50 hover:bg-accent-role-muted hover:text-accent-role"
    >
      <Icon className="size-3.5" strokeWidth={1.75} />
      {children}
    </Button>
  );
}
