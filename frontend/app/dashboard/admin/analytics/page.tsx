"use client";

import { useEffect, useMemo, useState } from "react";
import { BarChart3, Bot, Inbox, TrendingUp, Users } from "lucide-react";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import {
  chartAxisTick,
  chartBarCursor,
  chartDataTick,
  chartGridProps,
  chartLineCursor,
  chartTooltipLabelStyle,
  chartTooltipStyle,
} from "@/components/shared/chart-style";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminAnalyticsStore } from "@/stores/useAdminAnalyticsStore";

type Period = "monthly" | "quarter";

export default function AdminAnalyticsPage() {
  const [period, setPeriod] = useState<Period>("monthly");

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="font-heading text-xl font-semibold">Tahlil</h2>
            <p className="text-sm text-muted-foreground">
              Sinflar, o&apos;qituvchilar va AI foydalanish bo&apos;yicha chuqur tahlil.
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              variant={period === "monthly" ? "default" : "outline"}
              size="sm"
              onClick={() => setPeriod("monthly")}
            >
              Oylik
            </Button>
            <Button
              variant={period === "quarter" ? "default" : "outline"}
              size="sm"
              onClick={() => setPeriod("quarter")}
            >
              Choraklik
            </Button>
          </div>
        </div>

        {/* Keyed by period so the loading state resets on a fresh mount (isLoading starts
            true via useState) instead of a synchronous setState(true) inside the effect. */}
        <AnalyticsBody key={period} period={period} />
      </div>
    </DashboardShell>
  );
}

function AnalyticsBody({ period }: { period: Period }) {
  const classesComparison = useAdminAnalyticsStore((state) => state.classesComparison);
  const teachersRanking = useAdminAnalyticsStore((state) => state.teachersRanking);
  const schoolProgress = useAdminAnalyticsStore((state) => state.schoolProgress);
  const aiUsage = useAdminAnalyticsStore((state) => state.aiUsage);
  const fetchClassesComparison = useAdminAnalyticsStore((state) => state.fetchClassesComparison);
  const fetchTeachersRanking = useAdminAnalyticsStore((state) => state.fetchTeachersRanking);
  const fetchSchoolProgress = useAdminAnalyticsStore((state) => state.fetchSchoolProgress);
  const fetchAiUsage = useAdminAnalyticsStore((state) => state.fetchAiUsage);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetchClassesComparison(period),
      fetchTeachersRanking(),
      fetchSchoolProgress(period),
      fetchAiUsage(period),
    ])
      .catch(() => setError("Tahlil ma'lumotlarini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [period, fetchClassesComparison, fetchTeachersRanking, fetchSchoolProgress, fetchAiUsage]);

  const rankingData = useMemo(
    () =>
      teachersRanking.map((t) => ({
        name: `${t.firstName} ${t.lastName}`,
        avgGrade: t.avgGrade,
        gradedCount: t.gradedCount,
      })),
    [teachersRanking],
  );

  const progressData = useMemo(
    () =>
      schoolProgress.map((p) => ({
        label: new Date(p.periodStart).toLocaleDateString(),
        avgScore: p.avgScore,
        gradedCount: p.gradedCount,
      })),
    [schoolProgress],
  );

  return (
    <>
      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      <div className="stagger-rise space-y-6">
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="size-4" strokeWidth={1.75} />
                Sinflar taqqoslash
              </CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <ListSkeleton />
              ) : classesComparison.length === 0 ? (
                <EmptyState icon={Inbox} title="Ma'lumot yo'q" description="Ushbu davr uchun sinflar bo'yicha ma'lumot topilmadi." />
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={classesComparison} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                    <CartesianGrid {...chartGridProps} vertical={false} />
                    <XAxis
                      dataKey="className"
                      tick={chartAxisTick}
                      tickLine={false}
                      axisLine={{ stroke: "var(--border)" }}
                    />
                    <YAxis domain={[0, 100]} tick={chartDataTick} tickLine={false} axisLine={false} />
                    <Tooltip
                      contentStyle={chartTooltipStyle}
                      labelStyle={chartTooltipLabelStyle}
                      cursor={chartBarCursor}
                    />
                    <Bar
                      dataKey="avgScore"
                      name="O'rtacha ball"
                      unit="%"
                      fill="var(--color-chart-1)"
                      radius={[4, 4, 0, 0]}
                      maxBarSize={48}
                    />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Users className="size-4" strokeWidth={1.75} />
                O&apos;qituvchilar reytingi
              </CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <ListSkeleton />
              ) : rankingData.length === 0 ? (
                <EmptyState icon={Inbox} title="Ma'lumot yo'q" description="O'qituvchilar reytingi hali hisoblanmagan." />
              ) : (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart
                    data={rankingData}
                    layout="vertical"
                    margin={{ top: 4, right: 16, left: 8, bottom: 0 }}
                  >
                    <CartesianGrid {...chartGridProps} horizontal={false} />
                    <XAxis
                      type="number"
                      domain={[0, 5]}
                      tick={chartDataTick}
                      tickLine={false}
                      axisLine={false}
                    />
                    <YAxis
                      type="category"
                      dataKey="name"
                      width={130}
                      tick={chartAxisTick}
                      tickLine={false}
                      axisLine={{ stroke: "var(--border)" }}
                    />
                    <Tooltip
                      contentStyle={chartTooltipStyle}
                      labelStyle={chartTooltipLabelStyle}
                      cursor={chartBarCursor}
                    />
                    <Bar
                      dataKey="avgGrade"
                      name="O'rtacha baho"
                      unit="/5"
                      fill="var(--color-chart-2)"
                      radius={[0, 4, 4, 0]}
                      maxBarSize={22}
                    />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="size-4" strokeWidth={1.75} />
              Maktab bo&apos;yicha dinamika
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <ListSkeleton />
            ) : progressData.length === 0 ? (
              <EmptyState icon={Inbox} title="Ma'lumot yo'q" description="Ushbu davr uchun dinamika ma'lumoti topilmadi." />
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <AreaChart data={progressData} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid {...chartGridProps} vertical={false} />
                  <XAxis dataKey="label" tick={chartDataTick} tickLine={false} axisLine={{ stroke: "var(--border)" }} />
                  <YAxis domain={[0, 100]} tick={chartDataTick} tickLine={false} axisLine={false} />
                  <Tooltip
                    contentStyle={chartTooltipStyle}
                    labelStyle={chartTooltipLabelStyle}
                    cursor={chartLineCursor}
                  />
                  <Area
                    type="monotone"
                    dataKey="avgScore"
                    name="O'rtacha ball"
                    unit="%"
                    stroke="var(--color-chart-1)"
                    strokeWidth={2}
                    fill="var(--color-chart-1)"
                    fillOpacity={0.15}
                    activeDot={{ r: 4, fill: "var(--color-chart-1)", stroke: "var(--color-chart-1)" }}
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bot className="size-4" strokeWidth={1.75} />
              AI foydalanish (faqat baholash chaqiruvlari)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-6 sm:grid-cols-3">
              <div>
                <p className="mb-2 text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Sinflar bo&apos;yicha
                </p>
                {isLoading ? (
                  <ListSkeleton compact />
                ) : !aiUsage?.byClass.length ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : (
                  <ul className="space-y-2">
                    {aiUsage.byClass.map((c) => (
                      <li
                        key={c.classId}
                        className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                      >
                        <span>{c.className}</span>
                        <span className="font-data text-muted-foreground">{c.callCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <div>
                <p className="mb-2 text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Fanlar bo&apos;yicha
                </p>
                {isLoading ? (
                  <ListSkeleton compact />
                ) : !aiUsage?.bySubject.length ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : (
                  <ul className="space-y-2">
                    {aiUsage.bySubject.map((s) => (
                      <li
                        key={s.subjectId}
                        className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                      >
                        <span>{s.subjectName}</span>
                        <span className="font-data text-muted-foreground">{s.callCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <div>
                <p className="mb-2 text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  O&apos;qituvchilar bo&apos;yicha
                </p>
                {isLoading ? (
                  <ListSkeleton compact />
                ) : !aiUsage?.byTeacher.length ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : (
                  <ul className="space-y-2">
                    {aiUsage.byTeacher.map((t) => (
                      <li
                        key={t.teacherId}
                        className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                      >
                        <span>
                          {t.firstName} {t.lastName}
                        </span>
                        <span className="font-data text-muted-foreground">{t.callCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  );
}

function ListSkeleton({ compact }: { compact?: boolean }) {
  return (
    <div className="space-y-2">
      <Skeleton className={compact ? "h-8 w-full" : "h-10 w-full"} />
      <Skeleton className={compact ? "h-8 w-full" : "h-10 w-full"} />
      <Skeleton className={compact ? "h-8 w-full" : "h-10 w-full"} />
    </div>
  );
}
