"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle, ArrowRight, CheckCircle2, Eye, HeartPulse } from "lucide-react";
import { DashboardHero, HeroAction } from "@/components/shared/DashboardHero";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { StatTile, StatTileSkeleton } from "@/components/shared/StatTile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { usePsychologyStore } from "@/stores/usePsychologyStore";
import { useAuthStore } from "@/stores/useAuthStore";

export default function PsychologistDashboardPage() {
  const dashboard = usePsychologyStore((state) => state.dashboard);
  const fetchDashboard = usePsychologyStore((state) => state.fetchDashboard);
  const firstName = useAuthStore((state) => state.user?.firstName);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchDashboard()
      .catch(() => setError("Bosh sahifani yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchDashboard]);

  const heroDescription = dashboard
    ? `${dashboard.criticalSignals} kritik · ${dashboard.highSignals} yuqori · ${dashboard.mediumSignals} o'rta signal ochiq`
    : "Psixologik signallar va kuzatuv ro'yxati bo'yicha umumiy holat.";

  // Severity distribution — real fetched counts only; card hidden when there's
  // nothing to distribute.
  const signalTotal =
    (dashboard?.criticalSignals ?? 0) + (dashboard?.highSignals ?? 0) + (dashboard?.mediumSignals ?? 0);
  const pct = (count: number) => (signalTotal > 0 ? (count / signalTotal) * 100 : 0);

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <div className="space-y-6">
        <DashboardHero
          title={firstName ? `Salom, ${firstName}!` : "Salom!"}
          description={heroDescription}
          actions={
            <>
              <HeroAction href="/dashboard/psychologist/signals" icon={AlertTriangle}>
                Signallar
              </HeroAction>
              <HeroAction href="/dashboard/psychologist/watchlist" icon={Eye}>
                Kuzatuv ro&apos;yxati
              </HeroAction>
            </>
          }
        />

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
            <div className="stagger-rise grid grid-cols-2 gap-4 lg:grid-cols-4">
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

            {signalTotal > 0 ? (
              <Card>
                <CardHeader>
                  <CardTitle>Ochiq signallar taqsimoti</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="flex h-2.5 w-full overflow-hidden rounded-full">
                    {dashboard.criticalSignals > 0 ? (
                      <div
                        className="bg-severity-critical"
                        style={{ width: `${pct(dashboard.criticalSignals)}%` }}
                      />
                    ) : null}
                    {dashboard.highSignals > 0 ? (
                      <div
                        className="bg-severity-high"
                        style={{ width: `${pct(dashboard.highSignals)}%` }}
                      />
                    ) : null}
                    {dashboard.mediumSignals > 0 ? (
                      <div
                        className="bg-severity-medium"
                        style={{ width: `${pct(dashboard.mediumSignals)}%` }}
                      />
                    ) : null}
                  </div>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1.5">
                      <span className="bg-severity-critical size-2 rounded-full" />
                      Kritik ({dashboard.criticalSignals})
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="bg-severity-high size-2 rounded-full" />
                      Yuqori ({dashboard.highSignals})
                    </span>
                    <span className="flex items-center gap-1.5">
                      <span className="bg-severity-medium size-2 rounded-full" />
                      O&apos;rta ({dashboard.mediumSignals})
                    </span>
                  </div>
                </CardContent>
              </Card>
            ) : null}

            <div className="grid gap-4 lg:grid-cols-2">
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
                          className="card-lift hover-glow group flex items-center justify-between gap-3 rounded-md border border-border px-3 py-2.5 text-sm"
                        >
                          <span className="font-medium">{s.studentName}</span>
                          {s.reason ? (
                            <span className="text-muted-foreground"> — {s.reason}</span>
                          ) : null}
                          <HeartPulse
                            className="size-4 shrink-0 text-role-psychologist opacity-0 transition-opacity group-hover:opacity-100"
                            strokeWidth={1.75}
                          />
                        </li>
                      ))}
                    </ul>
                  )}
                </CardContent>
              </Card>

              <Card className="relative h-fit overflow-hidden border-role-psychologist/30">
                <span
                  aria-hidden
                  className="bg-role-psychologist/10 absolute -top-10 -right-10 size-32 rounded-full blur-2xl"
                />
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <HeartPulse className="size-4 text-role-psychologist" strokeWidth={1.75} />
                    Diqqat markazida
                  </CardTitle>
                </CardHeader>
                <CardContent className="relative space-y-3 text-sm">
                  <p className="text-muted-foreground">
                    Kritik signallar bo&apos;yicha ota-onalar avtomatik xabardor qilinadi va
                    signal hal qilingunicha kuzatuvda qoladi.
                  </p>
                  <Link
                    href="/dashboard/psychologist/signals"
                    className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
                  >
                    Barcha signallarni ko&apos;rish
                    <ArrowRight className="size-3.5" strokeWidth={1.75} />
                  </Link>
                </CardContent>
              </Card>
            </div>
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
