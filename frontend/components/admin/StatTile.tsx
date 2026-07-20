import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatTileProps {
  label: string;
  value: React.ReactNode;
  icon: LucideIcon;
  accentClassName?: string;
}

/** Admin dashboard stat tile — Card-based, role-admin accent by default, numeric value
 * in font-data per the design system's convention for verified/tabular numbers. */
export function StatTile({ label, value, icon: Icon, accentClassName }: StatTileProps) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 py-5">
        <span
          className={cn(
            "flex size-10 shrink-0 items-center justify-center rounded-lg bg-role-admin-muted text-role-admin",
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
