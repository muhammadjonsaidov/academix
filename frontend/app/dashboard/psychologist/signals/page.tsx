"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { PsychologistNav } from "@/components/psychologist/PsychologistNav";
import { usePsychologyStore } from "@/stores/usePsychologyStore";
import type { SignalSeverity } from "@/types/psychologist";

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

export default function PsychologistSignalsPage() {
  const signals = usePsychologyStore((state) => state.signals);
  const fetchSignals = usePsychologyStore((state) => state.fetchSignals);

  const [severityFilter, setSeverityFilter] = useState<SignalSeverity | "">("");
  const [resolvedFilter, setResolvedFilter] = useState<"" | "true" | "false">("false");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSignals({
      severity: severityFilter || undefined,
      resolved: resolvedFilter === "" ? undefined : resolvedFilter === "true",
    }).catch(() => setError("Signallarni yuklab bo'lmadi."));
  }, [fetchSignals, severityFilter, resolvedFilter]);

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <PsychologistNav />
      <h2 className="mb-4 text-lg font-semibold">Signallar</h2>

      <div className="mb-4 flex gap-4">
        <div className="space-y-1">
          <label htmlFor="severity" className="text-sm font-medium">
            Daraja
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
        <div className="space-y-1">
          <label htmlFor="resolved" className="text-sm font-medium">
            Holati
          </label>
          <select
            id="resolved"
            value={resolvedFilter}
            onChange={(e) => setResolvedFilter(e.target.value as "" | "true" | "false")}
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          >
            <option value="false">Ochiq</option>
            <option value="true">Hal qilingan</option>
            <option value="">Barchasi</option>
          </select>
        </div>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">O&apos;quvchi</th>
            <th className="py-2">Sinf</th>
            <th className="py-2">Turi</th>
            <th className="py-2">Daraja</th>
            <th className="py-2">Aniqlangan</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {signals.map((signal) => (
            <tr key={signal.signalId} className="border-b border-border">
              <td className="py-2 font-medium">
                {signal.studentName}
                {signal.isManipulation ? (
                  <span className="ml-1 text-xs text-destructive" title="Manipulyatsiya shubhasi">
                    ⚠
                  </span>
                ) : null}
              </td>
              <td className="py-2">{signal.className}</td>
              <td className="py-2">{TYPE_LABEL[signal.type] ?? signal.type}</td>
              <td className={`py-2 ${severityClass(signal.severity)}`}>
                {SEVERITY_LABEL[signal.severity]}
              </td>
              <td className="py-2">{new Date(signal.detectedAt).toLocaleString()}</td>
              <td className="py-2 text-right">
                <Link
                  href={`/dashboard/psychologist/signals/${signal.signalId}`}
                  className="text-sm underline"
                >
                  Ko&apos;rish
                </Link>
              </td>
            </tr>
          ))}
          {signals.length === 0 ? (
            <tr>
              <td colSpan={6} className="py-4 text-center text-muted-foreground">
                Signallar topilmadi.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
