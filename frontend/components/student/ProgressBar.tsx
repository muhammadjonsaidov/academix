import { cn } from "@/lib/utils";

interface ProgressBarProps {
  /** 0-100 */
  percent: number;
  className?: string;
}

/**
 * Role-student-tinted progress bar — used for XP-to-next-badge and per-subject average
 * score. Clamped to [0, 100] since backend percentages are never guaranteed to be exactly
 * in range (e.g. rounding on `submissionRate * 100`).
 */
export function ProgressBar({ percent, className }: ProgressBarProps) {
  const clamped = Math.min(100, Math.max(0, percent));
  return (
    <div
      className={cn("h-2 w-full overflow-hidden rounded-full bg-role-student-muted", className)}
      role="progressbar"
      aria-valuenow={Math.round(clamped)}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div
        className="h-full rounded-full bg-role-student transition-[width] duration-300"
        style={{ width: `${clamped}%` }}
      />
    </div>
  );
}
