"use client";

import { useEffect, useState } from "react";
import { Award, BarChart3, TrendingDown, TrendingUp } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/student/EmptyState";
import { ProgressBar } from "@/components/student/ProgressBar";
import { StatCard } from "@/components/student/StatCard";
import { useStudentStore } from "@/stores/useStudentStore";
import { cn } from "@/lib/utils";

export default function StudentProgressPage() {
  const progress = useStudentStore((state) => state.progress);
  const fetchProgress = useStudentStore((state) => state.fetchProgress);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchProgress().catch(() => setError("Progressni yuklab bo'lmadi."));
  }, [fetchProgress]);

  return (
    <DashboardShell role="STUDENT">
      <h2 className="mb-4 font-heading text-lg font-semibold">Mening progressim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {!progress ? (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Skeleton className="h-20" />
            <Skeleton className="h-20" />
          </div>
          <Skeleton className="h-40" />
        </div>
      ) : (
        <>
          <div className="mb-6 grid grid-cols-2 gap-4">
            <StatCard
              icon={BarChart3}
              value={`${progress.myGrowth.thisMonth.avgScore}%`}
              label={`Shu oy (o'tgan oy: ${progress.myGrowth.lastMonth.avgScore}%, ${progress.myGrowth.growth})`}
            />
            <StatCard icon={Award} value={progress.badges.length} label="Yutuqlar" />
          </div>

          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Fanlar bo&apos;yicha</CardTitle>
            </CardHeader>
            <CardContent>
              {progress.subjectStats.length > 0 ? (
                <ul className="space-y-4">
                  {progress.subjectStats.map((s) => {
                    const isUp = s.growth >= 0;
                    return (
                      <li key={s.subject}>
                        <div className="mb-1.5 flex items-center justify-between text-sm">
                          <span className="font-medium">{s.subject}</span>
                          <span
                            className={cn(
                              "font-data flex items-center gap-1",
                              isUp ? "text-success" : "text-destructive",
                            )}
                          >
                            {isUp ? (
                              <TrendingUp className="size-3.5" strokeWidth={1.75} />
                            ) : (
                              <TrendingDown className="size-3.5" strokeWidth={1.75} />
                            )}
                            {s.currentAvg}%
                          </span>
                        </div>
                        <ProgressBar percent={s.currentAvg} />
                        <p className="mt-1 text-xs text-muted-foreground">
                          O&apos;tgan oy: {s.previousMonthAvg}% — Topshirish darajasi:{" "}
                          {Math.round(s.submissionRate * 100)}%
                        </p>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <EmptyState
                  icon={BarChart3}
                  title="Ma'lumot yo'q"
                  description="Fanlar bo'yicha statistika baholangan ishlar bo'lgach ko'rinadi."
                />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>XP tarixi</CardTitle>
            </CardHeader>
            <CardContent>
              {progress.xpHistory.length > 0 ? (
                <ul className="space-y-2">
                  {progress.xpHistory.slice(0, 10).map((x, i) => (
                    <li
                      key={i}
                      className="flex items-center justify-between rounded-md bg-muted/40 px-3 py-2 text-sm"
                    >
                      <span>{x.reason}</span>
                      <span className="font-data text-muted-foreground">
                        +{x.xp} XP · {new Date(x.date).toLocaleDateString()}
                      </span>
                    </li>
                  ))}
                </ul>
              ) : (
                <EmptyState
                  icon={Award}
                  title="Ma'lumot yo'q"
                  description="XP tarixingiz shu yerda ko'rinadi."
                />
              )}
            </CardContent>
          </Card>
        </>
      )}
    </DashboardShell>
  );
}
