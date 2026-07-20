"use client";

import { useEffect, useState } from "react";
import { AlertTriangle, FileBarChart } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/psychologist/EmptyState";
import { SEVERITY_BADGE_VARIANT, SEVERITY_LABEL, TYPE_LABEL } from "@/components/psychologist/severity";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { usePsychologyStore } from "@/stores/usePsychologyStore";
import type { SignalSeverity, SignalType } from "@/types/psychologist";

function isSignalSeverity(value: string): value is SignalSeverity {
  return value === "LOW" || value === "MEDIUM" || value === "HIGH" || value === "CRITICAL";
}

export default function PsychologistReportsPage() {
  const report = usePsychologyStore((state) => state.monthlyReport);
  const fetchMonthlyReport = usePsychologyStore((state) => state.fetchMonthlyReport);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchMonthlyReport()
      .catch(() => setError("Hisobotni yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchMonthlyReport]);

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Oylik hisobot</h2>
          <p className="text-sm text-muted-foreground">
            Oxirgi 30 kunlik psixologik signallar statistikasi.
          </p>
        </div>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        {isLoading ? (
          <div className="space-y-4">
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-40 w-full" />
            <Skeleton className="h-40 w-full" />
          </div>
        ) : report ? (
          report.totalSignals === 0 ? (
            <EmptyState
              icon={FileBarChart}
              title="Ma'lumot yo'q"
              description="Oxirgi 30 kunda hech qanday psixologik signal qayd etilmagan."
            />
          ) : (
            <div className="space-y-6">
              <Card>
                <CardContent className="flex items-center gap-4 py-6">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-role-psychologist-muted text-role-psychologist">
                    <FileBarChart className="size-5" strokeWidth={1.75} />
                  </span>
                  <div>
                    <p className="font-data text-2xl leading-none font-semibold">
                      {report.totalSignals}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      Jami signal — {report.resolvedCount} ta hal qilingan,{" "}
                      {report.manipulationFlaggedCount} ta manipulyatsiya belgisi bilan
                    </p>
                  </div>
                </CardContent>
              </Card>

              <div className="grid gap-4 lg:grid-cols-2">
                <Card>
                  <CardHeader>
                    <CardTitle>Daraja bo&apos;yicha</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {Object.keys(report.bySeverity).length === 0 ? (
                      <EmptyState
                        icon={AlertTriangle}
                        title="Ma'lumot yo'q"
                        description="Daraja bo'yicha taqsimot hali mavjud emas."
                      />
                    ) : (
                      <ul className="space-y-2">
                        {Object.entries(report.bySeverity).map(([severity, count]) => (
                          <li
                            key={severity}
                            className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                          >
                            {isSignalSeverity(severity) ? (
                              <Badge variant={SEVERITY_BADGE_VARIANT[severity]}>
                                {SEVERITY_LABEL[severity]}
                              </Badge>
                            ) : (
                              <span className="font-medium">{severity}</span>
                            )}
                            <span className="font-data text-muted-foreground">{count}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Turi bo&apos;yicha</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {Object.keys(report.byType).length === 0 ? (
                      <EmptyState
                        icon={AlertTriangle}
                        title="Ma'lumot yo'q"
                        description="Tur bo'yicha taqsimot hali mavjud emas."
                      />
                    ) : (
                      <ul className="space-y-2">
                        {Object.entries(report.byType).map(([type, count]) => (
                          <li
                            key={type}
                            className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                          >
                            <span className="font-medium">
                              {TYPE_LABEL[type as SignalType] ?? type}
                            </span>
                            <span className="font-data text-muted-foreground">{count}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </CardContent>
                </Card>
              </div>
            </div>
          )
        ) : null}
      </div>
    </DashboardShell>
  );
}
