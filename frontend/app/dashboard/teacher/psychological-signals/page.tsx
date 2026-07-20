"use client";

import { useEffect, useState } from "react";
import { HeartPulse, MessageCircleWarning } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/teacher/EmptyState";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { SignalSeverity } from "@/types/teacher";

const SEVERITY_LABEL: Record<SignalSeverity, string> = {
  LOW: "Past",
  MEDIUM: "O'rta",
  HIGH: "Yuqori",
  CRITICAL: "Kritik",
};

const TYPE_LABEL: Record<string, string> = {
  LATE_NIGHT_ACTIVITY: "Tungi faollik",
  MOTIVATION_DROP: "Motivatsiya pasayishi",
  NEGATIVE_LANGUAGE: "Salbiy til",
  SUDDEN_PERFORMANCE_DROP: "Keskin pasayish",
  SUBMISSION_STOP: "Topshirish to'xtadi",
  AGGRESSIVE_LANGUAGE: "Tajovuzkor til",
  MANIPULATION_ATTEMPT: "Manipulyatsiya urinishi",
};

// Notify matrix (academix_tz.md §1.14): LOW = log only, MEDIUM/HIGH = teacher + psychologist,
// CRITICAL = + parent. Parents are never notified below CRITICAL, so this "Ota-onaga xabar
// berildi" line — and any future parent-notify action — must never render below CRITICAL.
function severityBadgeVariant(severity: SignalSeverity) {
  return `severity-${severity.toLowerCase()}` as
    | "severity-low"
    | "severity-medium"
    | "severity-high"
    | "severity-critical";
}

export default function TeacherPsychologicalSignalsPage() {
  const signals = useTeacherStore((state) => state.psychologicalSignals);
  const fetchPsychologicalSignals = useTeacherStore((state) => state.fetchPsychologicalSignals);
  const resolvePsychologicalSignal = useTeacherStore((state) => state.resolvePsychologicalSignal);

  const [severityFilter, setSeverityFilter] = useState<SignalSeverity | "">("");
  const [error, setError] = useState<string | null>(null);
  const [resolvingId, setResolvingId] = useState<string | null>(null);

  useEffect(() => {
    fetchPsychologicalSignals(severityFilter || undefined).catch(() =>
      setError("Signallarni yuklab bo'lmadi."),
    );
  }, [fetchPsychologicalSignals, severityFilter]);

  async function handleResolve(signalId: string) {
    setError(null);
    setResolvingId(signalId);
    try {
      await resolvePsychologicalSignal(signalId);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Hal qilib bo'lmadi.");
    } finally {
      setResolvingId(null);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-1 font-heading text-lg font-semibold">Psixologik signallar</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Faqat sinf rahbari sifatida biriktirilgan sinfingiz o&apos;quvchilari ko&apos;rinadi.
      </p>

      <div className="mb-4 space-y-1">
        <label htmlFor="severity" className="text-sm font-medium">
          Daraja bo&apos;yicha filter
        </label>
        <select
          id="severity"
          value={severityFilter}
          onChange={(e) => setSeverityFilter(e.target.value as SignalSeverity | "")}
          className="block rounded-md border border-input bg-background px-3 py-2 text-sm"
        >
          <option value="">Barchasi</option>
          {(Object.keys(SEVERITY_LABEL) as SignalSeverity[]).map((s) => (
            <option key={s} value={s}>
              {SEVERITY_LABEL[s]}
            </option>
          ))}
        </select>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {signals.length === 0 ? (
        <EmptyState
          icon={HeartPulse}
          title="Hozircha signallar yo'q"
          description="Sinfingiz o'quvchilari uchun psixologik signal aniqlanmagan."
        />
      ) : (
        <div className="space-y-3">
          {signals.map((signal) => (
            <Card
              key={signal.signalId}
              className={signal.severity === "CRITICAL" ? "rail-critical" : undefined}
            >
              <CardContent className="flex flex-wrap items-start justify-between gap-3">
                <div className="space-y-1.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-medium">{signal.studentName}</p>
                    <Badge variant={severityBadgeVariant(signal.severity)}>
                      {SEVERITY_LABEL[signal.severity]}
                    </Badge>
                    <span className="text-sm text-muted-foreground">
                      {TYPE_LABEL[signal.type] ?? signal.type}
                    </span>
                  </div>
                  {signal.description ? (
                    <p className="text-sm text-muted-foreground">{signal.description}</p>
                  ) : null}
                  {signal.severity === "CRITICAL" ? (
                    <p className="flex items-center gap-1.5 text-sm font-medium text-severity-critical">
                      <MessageCircleWarning className="size-3.5" strokeWidth={1.75} />
                      Ota-onaga xabar berildi
                    </p>
                  ) : null}
                  <p className="text-xs text-muted-foreground">
                    {signal.resolved
                      ? `Hal qilingan${signal.resolvedAt ? ` — ${new Date(signal.resolvedAt).toLocaleString()}` : ""}`
                      : `Aniqlangan — ${new Date(signal.detectedAt).toLocaleString()}`}
                  </p>
                </div>
                {!signal.resolved ? (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={resolvingId === signal.signalId}
                    onClick={() => handleResolve(signal.signalId)}
                  >
                    {resolvingId === signal.signalId ? "..." : "Hal qilish"}
                  </Button>
                ) : (
                  <Badge variant="ready">Hal qilingan</Badge>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
