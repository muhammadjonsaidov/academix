import { useInView } from "@/hooks/useInView";
import { cn } from "@/lib/utils";

interface ProgressBarProps {
  /** 0–100. Clamped at render, so callers can pass raw values (e.g. avgScore). */
  value: number;
  /** role = the dashboard's role accent (default), success = chalk-green for
   *  graded/verified data, destructive = pen-red for at-risk values. */
  tone?: "role" | "success" | "destructive";
  className?: string;
}

/**
 * Thin progress bar — the data-viz companion to font-data numbers. Fill tone
 * defaults to the shell's role accent (data-role layer), so list rows on every
 * dashboard pick up their own hue automatically; semantic values pass an
 * explicit tone (success for grades, destructive for at-risk percentages).
 * The fill animates 0 → value the first time the bar scrolls into view.
 */
export function ProgressBar({ value, tone = "role", className }: ProgressBarProps) {
  const clamped = Math.max(0, Math.min(100, value));
  const { ref, inView } = useInView<HTMLDivElement>();
  const fill =
    tone === "success" ? "bg-success" : tone === "destructive" ? "bg-destructive" : "bg-accent-role";
  return (
    <div
      ref={ref}
      role="progressbar"
      aria-valuenow={Math.round(clamped)}
      aria-valuemin={0}
      aria-valuemax={100}
      className={cn("h-1.5 w-full overflow-hidden rounded-full bg-muted", className)}
    >
      <div
        className={cn("h-full rounded-full transition-[width] duration-700 ease-out", fill)}
        style={{ width: inView ? `${clamped}%` : "0%" }}
      />
    </div>
  );
}
