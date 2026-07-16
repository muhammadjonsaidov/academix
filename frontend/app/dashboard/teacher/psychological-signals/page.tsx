"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { Button } from "@/components/ui/button";
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

function severityClass(severity: SignalSeverity) {
  if (severity === "CRITICAL") return "text-destructive font-semibold";
  if (severity === "HIGH") return "text-amber-600 font-medium";
  return "text-muted-foreground";
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
      <TeacherNav />
      <h2 className="mb-1 text-lg font-semibold">Psixologik signallar</h2>
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
          className="rounded-md border border-input bg-background px-3 py-2 text-sm"
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

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">O&apos;quvchi</th>
            <th className="py-2">Turi</th>
            <th className="py-2">Daraja</th>
            <th className="py-2">Tavsif</th>
            <th className="py-2">Holati</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {signals.map((signal) => (
            <tr key={signal.signalId} className="border-b border-border">
              <td className="py-2 font-medium">{signal.studentName}</td>
              <td className="py-2">{TYPE_LABEL[signal.type] ?? signal.type}</td>
              <td className={`py-2 ${severityClass(signal.severity)}`}>
                {SEVERITY_LABEL[signal.severity]}
              </td>
              <td className="py-2">{signal.description ?? "—"}</td>
              <td className="py-2">
                {signal.resolved ? "Hal qilingan" : "Ochiq"}
              </td>
              <td className="py-2 text-right">
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
                ) : null}
              </td>
            </tr>
          ))}
          {signals.length === 0 ? (
            <tr>
              <td colSpan={6} className="py-4 text-center text-muted-foreground">
                Hozircha signallar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
