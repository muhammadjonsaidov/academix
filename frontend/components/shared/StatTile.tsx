import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatTileProps {
  label: string;
  value: React.ReactNode;
  icon: LucideIcon;
  /** Icon-chip background/foreground, e.g. "bg-role-admin-muted text-role-admin" or a
   * severity accent like "bg-severity-critical text-severity-critical-foreground". */
  accentClassName: string;
  /** Applied to the outer Card — e.g. "rail-critical" so the signature verification rail
   * carries extra visual weight beyond the icon chip alone. */
  className?: string;
}

/** Dashboard stat tile — Card-based icon chip + font-data numeric value, shared across
 * every role dashboard. Accent color is always explicit at the call site (no hidden
 * per-role default) so severity/role tiles read the same way in the source. */
export function StatTile({ label, value, icon: Icon, accentClassName, className }: StatTileProps) {
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
        <div>
          <p className="font-data text-2xl leading-none font-semibold">{value}</p>
          <p className="mt-1 text-sm text-muted-foreground">{label}</p>
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
