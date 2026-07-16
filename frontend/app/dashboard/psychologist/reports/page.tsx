"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { PsychologistNav } from "@/components/psychologist/PsychologistNav";
import { usePsychologyStore } from "@/stores/usePsychologyStore";

export default function PsychologistReportsPage() {
  const report = usePsychologyStore((state) => state.monthlyReport);
  const fetchMonthlyReport = usePsychologyStore((state) => state.fetchMonthlyReport);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchMonthlyReport().catch(() => setError("Hisobotni yuklab bo'lmadi."));
  }, [fetchMonthlyReport]);

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <PsychologistNav />
      <h2 className="mb-4 text-lg font-semibold">Oylik hisobot</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {report ? (
        <div className="space-y-6">
          <div className="rounded-md border border-border p-4">
            <p className="text-2xl font-semibold">{report.totalSignals}</p>
            <p className="text-sm text-muted-foreground">
              Jami signal (oxirgi 30 kun) — {report.resolvedCount} ta hal qilingan,{" "}
              {report.manipulationFlaggedCount} ta manipulyatsiya belgisi bilan
            </p>
          </div>

          <div>
            <h3 className="mb-2 text-sm font-semibold">Daraja bo&apos;yicha</h3>
            <ul className="space-y-1 text-sm">
              {Object.entries(report.bySeverity).map(([severity, count]) => (
                <li key={severity}>
                  {severity}: {count}
                </li>
              ))}
              {Object.keys(report.bySeverity).length === 0 ? (
                <li className="text-muted-foreground">Ma&apos;lumot yo&apos;q.</li>
              ) : null}
            </ul>
          </div>

          <div>
            <h3 className="mb-2 text-sm font-semibold">Turi bo&apos;yicha</h3>
            <ul className="space-y-1 text-sm">
              {Object.entries(report.byType).map(([type, count]) => (
                <li key={type}>
                  {type}: {count}
                </li>
              ))}
              {Object.keys(report.byType).length === 0 ? (
                <li className="text-muted-foreground">Ma&apos;lumot yo&apos;q.</li>
              ) : null}
            </ul>
          </div>
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Yuklanmoqda...</p>
      )}
    </DashboardShell>
  );
}
