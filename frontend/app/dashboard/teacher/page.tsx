"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ClipboardCheck, GraduationCap, School, Star, TrendingUp } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import { StatTile, StatTileSkeleton, type StatTileDelta } from "@/components/shared/StatTile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTeacherAnalyticsStore } from "@/stores/useTeacherAnalyticsStore";

/** Turns the already-fetched rating trend string ("-0.5", "+0.3", "0", ...) into a
 * StatTile delta chip — sign decides tone; an empty trend renders no chip at all. */
function ratingDelta(trend: string): StatTileDelta | undefined {
  const trimmed = trend.trim();
  if (!trimmed) return undefined;
  const numeric = Number.parseFloat(trimmed);
  const tone: StatTileDelta["tone"] =
    Number.isNaN(numeric) || numeric === 0 ? "neutral" : numeric > 0 ? "positive" : "negative";
  return { label: trimmed, tone };
}

export default function TeacherDashboardPage() {
  const dashboard = useTeacherAnalyticsStore((state) => state.dashboard);
  const fetchDashboard = useTeacherAnalyticsStore((state) => state.fetchDashboard);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Bosh sahifani yuklab bo'lmadi."));
  }, [fetchDashboard]);

  return (
    <DashboardShell role="TEACHER">
      <div className="space-y-6">
        <PageHeader
          title="Bosh sahifa"
          description="Sinflaringiz, baholash jarayoni va reyting bo'yicha umumiy holat."
        />

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        {!dashboard && !error ? (
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatTileSkeleton />
            <StatTileSkeleton />
            <StatTileSkeleton />
            <StatTileSkeleton />
          </div>
        ) : null}

        {dashboard ? (
          <>
            <div className="stagger-rise grid grid-cols-2 gap-4 lg:grid-cols-4">
              <StatTile
                label="Kutilayotgan ishlar"
                value={dashboard.pendingSubmissions}
                icon={ClipboardCheck}
              />
              <StatTile label="Bugun baholandi" value={dashboard.gradedToday} icon={GraduationCap} />
              <StatTile
                label="Reyting"
                value={`${dashboard.myRating.score}/5`}
                icon={Star}
                delta={ratingDelta(dashboard.myRating.trend)}
              />
              <StatTile label="Sinflarim" value={dashboard.myClasses.length} icon={School} />
            </div>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <TrendingUp className="size-4 text-muted-foreground" strokeWidth={1.75} />
                  Sinflar bo&apos;yicha o&apos;zlashtirish
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {dashboard.classProgressSummary.map((c) => (
                  <Link
                    key={c.classId}
                    href={`/dashboard/teacher/classes/${c.classId}/analytics`}
                    className="card-lift flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm transition-colors hover:bg-muted/60"
                  >
                    <span className="font-medium">{c.className}</span>
                    <span className="font-data text-muted-foreground">
                      {c.avgScore}% &middot; {c.gradedCount} baholangan &middot; {c.studentCount}{" "}
                      o&apos;quvchi
                    </span>
                  </Link>
                ))}
                {dashboard.classProgressSummary.length === 0 ? (
                  <EmptyState
                    icon={School}
                    title="Ma'lumot yo'q"
                    description="Sinflaringiz bo'yicha o'zlashtirish ma'lumotlari hali mavjud emas."
                  />
                ) : null}
              </CardContent>
            </Card>
          </>
        ) : null}
      </div>
    </DashboardShell>
  );
}
