"use client";

import { useEffect, useMemo, useState } from "react";
import { Award, BarChart3, TrendingDown, TrendingUp } from "lucide-react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import {
  chartAxisTick,
  chartDataTick,
  chartGridProps,
  chartLegendStyle,
  chartLineCursor,
  chartTooltipLabelStyle,
  chartTooltipStyle,
} from "@/components/shared/chart-style";
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

  // Cumulative XP over time, aggregated per day so a busy day is one point, not many.
  const xpTrend = useMemo(() => {
    const history = progress?.xpHistory ?? [];
    const byDay = new Map<string, number>();
    for (const item of history) {
      const day = item.date.slice(0, 10);
      byDay.set(day, (byDay.get(day) ?? 0) + item.xp);
    }
    const trend: { label: string; totalXp: number }[] = [];
    let total = 0;
    for (const [day, xp] of [...byDay.entries()].sort(([a], [b]) => a.localeCompare(b))) {
      total += xp;
      trend.push({ label: new Date(day).toLocaleDateString(), totalXp: total });
    }
    return trend;
  }, [progress?.xpHistory]);

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
              {/* Radar only makes sense as a polygon — needs at least 3 axes. */}
              {progress.subjectStats.length >= 3 ? (
                <ResponsiveContainer width="100%" height={260} className="mb-4">
                  <RadarChart data={progress.subjectStats} outerRadius="70%">
                    <PolarGrid stroke="var(--border)" />
                    <PolarAngleAxis dataKey="subject" tick={chartAxisTick} />
                    <PolarRadiusAxis
                      domain={[0, 100]}
                      tick={{ ...chartDataTick, fontSize: 10 }}
                      stroke="var(--border)"
                      axisLine={false}
                    />
                    <Radar
                      name="Joriy o'rtacha"
                      dataKey="currentAvg"
                      stroke="var(--color-chart-3)"
                      strokeWidth={2}
                      fill="var(--color-chart-3)"
                      fillOpacity={0.25}
                    />
                    <Radar
                      name="O'tgan oy"
                      dataKey="previousMonthAvg"
                      stroke="var(--graphite)"
                      fill="var(--graphite)"
                      fillOpacity={0.1}
                    />
                    <Legend wrapperStyle={chartLegendStyle} />
                    <Tooltip contentStyle={chartTooltipStyle} labelStyle={chartTooltipLabelStyle} />
                  </RadarChart>
                </ResponsiveContainer>
              ) : null}
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
                            {s.currentAvg.toFixed(1)}%
                          </span>
                        </div>
                        <ProgressBar percent={s.currentAvg} />
                        <p className="mt-1 text-xs text-muted-foreground">
                          O&apos;tgan oy: {s.previousMonthAvg.toFixed(1)}% — Topshirish darajasi:{" "}
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
              {/* A single point can't draw a curve — the recent list below covers that case. */}
              {xpTrend.length >= 2 ? (
                <ResponsiveContainer width="100%" height={220} className="mb-4">
                  <AreaChart data={xpTrend} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                    <CartesianGrid {...chartGridProps} vertical={false} />
                    <XAxis
                      dataKey="label"
                      tick={chartDataTick}
                      tickLine={false}
                      axisLine={{ stroke: "var(--border)" }}
                    />
                    <YAxis tick={chartDataTick} tickLine={false} axisLine={false} />
                    <Tooltip
                      contentStyle={chartTooltipStyle}
                      labelStyle={chartTooltipLabelStyle}
                      cursor={chartLineCursor}
                    />
                    <Area
                      type="monotone"
                      dataKey="totalXp"
                      name="Jami XP"
                      unit=" XP"
                      stroke="var(--color-chart-3)"
                      strokeWidth={2}
                      fill="var(--color-chart-3)"
                      fillOpacity={0.15}
                      activeDot={{ r: 4, fill: "var(--color-chart-3)", stroke: "var(--color-chart-3)" }}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              ) : null}
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
