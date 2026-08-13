import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { useCountUp } from "@/hooks/useCountUp";
import { cn } from "@/lib/utils";

interface StatCardProps {
  icon: LucideIcon;
  value: React.ReactNode;
  label: string;
  className?: string;
}

/**
 * Gamification stat tile (XP, streak, badge count, etc.) — big `font-data` number with a
 * role-tinted icon chip, more energetic than a bare bordered box. Used on the student
 * dashboard/progress pages; not shared cross-role since other dashboards' stat tiles are
 * out of scope here.
 */
export function StatCard({ icon: Icon, value, label, className }: StatCardProps) {
  // Only pure numbers count up; formatted strings render as-is.
  const animated =
    typeof value === "number" && Number.isFinite(value) ? (
      <AnimatedNumber value={value} />
    ) : (
      value
    );
  return (
    <Card className={cn("card-lift hover-glow relative overflow-hidden", className)}>
      {/* Soft role-accent corner wash — matches StatTile's treatment */}
      <span
        aria-hidden
        className="bg-accent-role/10 absolute -top-8 -right-8 size-24 rounded-full blur-2xl"
      />
      <CardContent className="relative flex items-center gap-4 py-5">
        {/* Chip hue comes from the shell's data-role accent layer (resolves to the
            student tokens here) — no hardcoded role class. */}
        <span className="bg-accent-role-muted text-accent-role flex size-11 shrink-0 items-center justify-center rounded-full ring-1 ring-current">
          <Icon className="size-5" strokeWidth={1.75} />
        </span>
        <div className="min-w-0">
          <p className="font-data truncate text-3xl leading-none font-semibold">{animated}</p>
          <p className="mt-1.5 text-xs leading-snug text-muted-foreground">{label}</p>
        </div>
      </CardContent>
    </Card>
  );
}

function AnimatedNumber({ value }: { value: number }) {
  const { ref, value: count } = useCountUp(value);
  return <span ref={ref}>{Math.round(count)}</span>;
}
