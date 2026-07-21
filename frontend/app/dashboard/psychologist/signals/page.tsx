"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { AlertTriangle } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { CriticalNotifyNote, SEVERITY_BADGE_VARIANT, SEVERITY_LABEL, TYPE_LABEL } from "@/components/psychologist/severity";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { usePsychologyStore } from "@/stores/usePsychologyStore";
import type { SignalSeverity } from "@/types/psychologist";

/**
 * Backend filter (`GET /psychologist/signals?severity=`) only accepts a single severity
 * value or none — there's no "all except LOW" server-side filter. "ACTIONABLE" is a
 * UI-only default: fetch unfiltered, then hide LOW client-side, so LOW stays reachable via
 * the same dropdown (select "Past" or "Barchasi") without a second server call shape.
 */
type SeverityFilterValue = SignalSeverity | "ALL" | "ACTIONABLE";

const SEVERITY_OPTIONS: { value: SeverityFilterValue; label: string }[] = [
  { value: "ACTIONABLE", label: "Muhim (past darajadan tashqari)" },
  { value: "ALL", label: "Barchasi" },
  { value: "LOW", label: SEVERITY_LABEL.LOW },
  { value: "MEDIUM", label: SEVERITY_LABEL.MEDIUM },
  { value: "HIGH", label: SEVERITY_LABEL.HIGH },
  { value: "CRITICAL", label: SEVERITY_LABEL.CRITICAL },
];

export default function PsychologistSignalsPage() {
  const signals = usePsychologyStore((state) => state.signals);
  const fetchSignals = usePsychologyStore((state) => state.fetchSignals);

  // Default excludes LOW (log-only, not action-relevant) per the flow spec — LOW stays
  // reachable via the dropdown, just not the default view.
  const [severityFilter, setSeverityFilter] = useState<SeverityFilterValue>("ACTIONABLE");
  const [resolvedFilter, setResolvedFilter] = useState<"" | "true" | "false">("false");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const serverSeverity =
      severityFilter === "ALL" || severityFilter === "ACTIONABLE" ? undefined : severityFilter;
    fetchSignals({
      severity: serverSeverity,
      resolved: resolvedFilter === "" ? undefined : resolvedFilter === "true",
    })
      .catch(() => setError("Signallarni yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchSignals, severityFilter, resolvedFilter]);

  function handleSeverityChange(value: SeverityFilterValue) {
    setIsLoading(true);
    setSeverityFilter(value);
  }

  function handleResolvedChange(value: "" | "true" | "false") {
    setIsLoading(true);
    setResolvedFilter(value);
  }

  const visibleSignals = useMemo(() => {
    if (severityFilter === "ACTIONABLE") {
      return signals.filter((s) => s.severity !== "LOW");
    }
    return signals;
  }, [signals, severityFilter]);

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Signallar</h2>
          <p className="text-sm text-muted-foreground">
            Aniqlangan psixologik signallar ro&apos;yxati.
          </p>
        </div>

        <div className="flex flex-wrap gap-4">
          <FormField label="Daraja" htmlFor="severity">
            <select
              id="severity"
              value={severityFilter}
              onChange={(e) => handleSeverityChange(e.target.value as SeverityFilterValue)}
              className={fieldClass}
            >
              {SEVERITY_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Holati" htmlFor="resolved">
            <select
              id="resolved"
              value={resolvedFilter}
              onChange={(e) => handleResolvedChange(e.target.value as "" | "true" | "false")}
              className={fieldClass}
            >
              <option value="false">Ochiq</option>
              <option value="true">Hal qilingan</option>
              <option value="">Barchasi</option>
            </select>
          </FormField>
        </div>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        <Card>
          <CardContent className="pt-6">
            {isLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : visibleSignals.length === 0 ? (
              <EmptyState
                icon={AlertTriangle}
                title="Signallar topilmadi"
                description="Tanlangan filtrlarga mos psixologik signal mavjud emas."
              />
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">O&apos;quvchi</th>
                    <th className="py-2 font-medium">Sinf</th>
                    <th className="py-2 font-medium">Turi</th>
                    <th className="py-2 font-medium">Daraja</th>
                    <th className="py-2 font-medium">Aniqlangan</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {visibleSignals.map((signal) => (
                    <tr
                      key={signal.signalId}
                      className={
                        signal.severity === "CRITICAL"
                          ? "border-b border-border bg-severity-critical/5 last:border-0"
                          : "border-b border-border last:border-0"
                      }
                    >
                      <td
                        className={
                          signal.severity === "CRITICAL"
                            ? "rail-critical py-2.5 pl-3 font-medium"
                            : "py-2.5 font-medium"
                        }
                      >
                        {signal.studentName}
                        {signal.isManipulation ? (
                          <span
                            className="ml-1.5 text-xs text-severity-high"
                            title="Manipulyatsiya shubhasi"
                          >
                            ⚠
                          </span>
                        ) : null}
                        {signal.severity === "CRITICAL" ? (
                          <CriticalNotifyNote className="mt-0.5 text-xs font-normal text-severity-critical" />
                        ) : null}
                      </td>
                      <td className="py-2.5">{signal.className}</td>
                      <td className="py-2.5">{TYPE_LABEL[signal.type] ?? signal.type}</td>
                      <td className="py-2.5">
                        <Badge variant={SEVERITY_BADGE_VARIANT[signal.severity]}>
                          {SEVERITY_LABEL[signal.severity]}
                        </Badge>
                      </td>
                      <td className="py-2.5 font-data text-muted-foreground">
                        {new Date(signal.detectedAt).toLocaleString()}
                      </td>
                      <td className="py-2.5 text-right">
                        <Link
                          href={`/dashboard/psychologist/signals/${signal.signalId}`}
                          className="text-sm font-medium text-primary hover:underline"
                        >
                          Ko&apos;rish
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardShell>
  );
}
