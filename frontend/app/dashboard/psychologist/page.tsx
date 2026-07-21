"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle, ArrowRight, CheckCircle2, Eye, HeartPulse } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { StatTile, StatTileSkeleton } from "@/components/shared/StatTile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { usePsychologyStore } from "@/stores/usePsychologyStore";

export default function PsychologistDashboardPage() {
  const dashboard = usePsychologyStore((state) => state.dashboard);
  const fetchDashboard = usePsychologyStore((state) => state.fetchDashboard);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchDashboard()
      .catch(() => setError("Bosh sahifani yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchDashboard]);

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Bosh sahifa</h2>
          <p className="text-sm text-muted-foreground">
            Psixologik signallar va kuzatuv ro&apos;yxati bo&apos;yicha umumiy holat.
          </p>
        </div>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        {isLoading ? (
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatTileSkeleton />
            <StatTileSkeleton />
            <StatTileSkeleton />
            <StatTileSkeleton />
          </div>
        ) : dashboard ? (
          <>
            <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
              <StatTile
                label="Kritik"
                value={dashboard.criticalSignals}
                icon={AlertTriangle}
                accentClassName="bg-severity-critical text-severity-critical-foreground"
                className="rail-critical"
              />
              <StatTile
                label="Yuqori"
                value={dashboard.highSignals}
                icon={AlertTriangle}
                accentClassName="bg-severity-high-bg text-severity-high"
              />
              <StatTile
                label="O'rta"
                value={dashboard.mediumSignals}
                icon={AlertTriangle}
                accentClassName="bg-severity-medium-bg text-severity-medium"
              />
              <StatTile
                label="Shu hafta hal qilindi"
                value={dashboard.resolvedThisWeek}
                icon={CheckCircle2}
                accentClassName="bg-success-bg text-success"
              />
            </div>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Eye className="size-4" strokeWidth={1.75} />
                  Kuzatuv ro&apos;yxati
                </CardTitle>
              </CardHeader>
              <CardContent>
                {dashboard.watchlistStudents.length === 0 ? (
                  <EmptyState
                    icon={Eye}
                    title="Kuzatuv ro'yxati bo'sh"
                    description="Hozircha maxsus kuzatuv talab qiladigan o'quvchi belgilanmagan."
                  />
                ) : (
                  <ul className="space-y-2">
                    {dashboard.watchlistStudents.map((s) => (
                      <li
                        key={s.studentId}
                        className="rounded-md border border-border px-3 py-2.5 text-sm"
                      >
                        <span className="font-medium">{s.studentName}</span>
                        {s.reason ? (
                          <span className="text-muted-foreground"> — {s.reason}</span>
                        ) : null}
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>

            <Link
              href="/dashboard/psychologist/signals"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
            >
              Barcha signallarni ko&apos;rish
              <ArrowRight className="size-3.5" strokeWidth={1.75} />
            </Link>
          </>
        ) : (
          <EmptyState
            icon={HeartPulse}
            title="Ma'lumot yo'q"
            description="Bosh sahifa ma'lumotlarini hozircha yuklab bo'lmadi."
          />
        )}
      </div>
    </DashboardShell>
  );
}
