import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

/** Small trend/secondary-value chip next to the main number. Tone is decided at the
 * call site (e.g. by the sign of an already-fetched delta) — positive reads
 * chalk-green, negative reads pen-red, neutral stays graphite. These are SEMANTIC
 * colors, deliberately not the role accent. */
export interface StatTileDelta {
  label: string;
  tone: "positive" | "negative" | "neutral";
}

const DELTA_TONE_CLASSES: Record<StatTileDelta["tone"], string> = {
  positive: "bg-success-bg text-success",
  negative: "bg-destructive/10 text-destructive",
  neutral: "bg-muted text-muted-foreground",
};

interface StatTileProps {
  label: string;
  value: React.ReactNode;
  icon: LucideIcon;
  /** Icon-chip background/foreground. Defaults to the current dashboard's role accent
   * (via the data-role layer in globals.css) so every role's tiles pick up their own
   * hue automatically; pass an explicit semantic accent to override, e.g.
   * "bg-severity-critical text-severity-critical-foreground" for severity tiles. */
  accentClassName?: string;
  /** Optional secondary value (trend/delta) rendered as a small colored chip beside
   * the number — only for values already present in fetched data, never invented. */
  delta?: StatTileDelta;
  /** Applied to the outer Card — e.g. "rail-critical" so the signature verification rail
   * carries extra visual weight beyond the icon chip alone. */
  className?: string;
}

/** Dashboard stat tile — Card-based icon chip + font-data numeric value, shared across
 * every role dashboard. Role tiles need no accent prop (role-adaptive by default);
 * severity/status tiles keep passing their semantic accent explicitly. */
export function StatTile({
  label,
  value,
  icon: Icon,
  accentClassName = "bg-accent-role-muted text-accent-role",
  delta,
  className,
}: StatTileProps) {
  return (
    <Card className={className}>
      <CardContent className="flex items-center gap-4 py-5">
        <span
          className={cn(
            "flex size-10 shrink-0 items-center justify-center rounded-lg",
            accentClassName,
          )}
        >
          <Icon className="size-5" strokeWidth={1.75} />
        </span>
        <div className="min-w-0">
          <div className="flex items-baseline gap-2">
            <p className="font-data text-2xl leading-none font-semibold">{value}</p>
            {delta ? (
              <span
                className={cn(
                  "font-data inline-flex shrink-0 items-center rounded-full px-1.5 py-0.5 text-xs font-medium",
                  DELTA_TONE_CLASSES[delta.tone],
                )}
              >
                {delta.label}
              </span>
            ) : null}
          </div>
          <p className="mt-1.5 text-xs leading-snug text-muted-foreground">{label}</p>
        </div>
      </CardContent>
    </Card>
  );
}

export function StatTileSkeleton() {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 py-5">
        <Skeleton className="size-10 shrink-0 rounded-lg" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-6 w-16" />
          <Skeleton className="h-3 w-24" />
        </div>
      </CardContent>
    </Card>
  );
}
