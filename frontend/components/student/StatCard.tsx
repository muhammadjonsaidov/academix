import type { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
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
  return (
    <Card className={cn(className)}>
      <CardContent className="flex items-center gap-3">
        {/* Chip hue comes from the shell's data-role accent layer (resolves to the
            student tokens here) — no hardcoded role class. */}
        <span className="bg-accent-role-muted text-accent-role flex size-10 shrink-0 items-center justify-center rounded-full">
          <Icon className="size-5" strokeWidth={1.75} />
        </span>
        <div className="min-w-0">
          <p className="font-data truncate text-2xl leading-none font-semibold">{value}</p>
          <p className="mt-1.5 truncate text-xs text-muted-foreground">{label}</p>
        </div>
      </CardContent>
    </Card>
  );
}
