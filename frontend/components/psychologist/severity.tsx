import type { SignalSeverity, SignalType } from "@/types/psychologist";

export const SEVERITY_LABEL: Record<SignalSeverity, string> = {
  LOW: "Past",
  MEDIUM: "O'rta",
  HIGH: "Yuqori",
  CRITICAL: "Kritik",
};

/** Maps a signal severity straight onto the Badge primitive's severity-* variant set
 * (components/ui/badge.tsx) — CRITICAL is the only solid-fill tier. */
export const SEVERITY_BADGE_VARIANT: Record<
  SignalSeverity,
  "severity-low" | "severity-medium" | "severity-high" | "severity-critical"
> = {
  LOW: "severity-low",
  MEDIUM: "severity-medium",
  HIGH: "severity-high",
  CRITICAL: "severity-critical",
};

export const TYPE_LABEL: Record<SignalType, string> = {
  LATE_NIGHT_ACTIVITY: "Tungi faollik",
  MOTIVATION_DROP: "Motivatsiya pasayishi",
  NEGATIVE_LANGUAGE: "Salbiy til",
  SUDDEN_PERFORMANCE_DROP: "Keskin pasayish",
  SUBMISSION_STOP: "Topshirish to'xtadi",
  AGGRESSIVE_LANGUAGE: "Tajovuzkor til",
  MANIPULATION_ATTEMPT: "Manipulyatsiya urinishi",
};

/**
 * Notify-matrix legibility fix (ui-ux-designer flow spec): CRITICAL is the only severity
 * tier that reaches the parent (LOW=log only, MEDIUM/HIGH=teacher+psychologist,
 * CRITICAL=+parent — academix_tz.md §1.14). This is display-only, driven purely by the
 * backend-computed `severity` field — there is deliberately no UI affordance anywhere to
 * manually notify a parent below CRITICAL, which would bypass the backend notify matrix.
 */
export function CriticalNotifyNote({ className }: { className?: string }) {
  return (
    <p className={className ?? "text-xs font-medium text-severity-critical"}>
      Ota-onaga xabar berildi
    </p>
  );
}
