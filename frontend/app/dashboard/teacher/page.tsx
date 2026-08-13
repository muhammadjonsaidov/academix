"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  BookOpen,
  ClipboardCheck,
  GraduationCap,
  School,
  Star,
  TrendingUp,
} from "lucide-react";
import { DashboardHero, HeroAction } from "@/components/shared/DashboardHero";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { ProgressBar } from "@/components/shared/ProgressBar";
import { StatTile, StatTileSkeleton, type StatTileDelta } from "@/components/shared/StatTile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTeacherAnalyticsStore } from "@/stores/useTeacherAnalyticsStore";
import { useAuthStore } from "@/stores/useAuthStore";

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
  const firstName = useAuthStore((state) => state.user?.firstName);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Bosh sahifani yuklab bo'lmadi."));
  }, [fetchDashboard]);

  const heroDescription = dashboard
    ? `${dashboard.pendingSubmissions} kutilayotgan ish · bugun ${dashboard.gradedToday} baholandi · ${dashboard.myClasses.length} sinf`
    : "Sinflaringiz, baholash jarayoni va reyting bo'yicha umumiy holat.";

  return (
    <DashboardShell role="TEACHER">
      <div className="space-y-6">
        <DashboardHero
          title={firstName ? `Salom, ${firstName}!` : "Salom!"}
          description={heroDescription}
          actions={
            <>
              <HeroAction href="/dashboard/teacher/homework" icon={BookOpen}>
                Yangi vazifa
              </HeroAction>
              <HeroAction href="/dashboard/teacher/submissions" icon={ClipboardCheck}>
                Ishlarni baholash
              </HeroAction>
              <HeroAction href="/dashboard/teacher/classes" icon={School}>
                Sinflarim
              </HeroAction>
            </>
          }
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
                    className="card-lift hover-glow block space-y-1.5 rounded-md border border-border px-3 py-2.5 text-sm transition-colors"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <span className="truncate font-medium">{c.className}</span>
                      <span className="font-data shrink-0 text-muted-foreground">{c.avgScore}%</span>
                    </div>
                    <ProgressBar
                      value={c.avgScore}
                      tone={c.avgScore >= 70 ? "success" : c.avgScore >= 50 ? "role" : "destructive"}
                    />
                    <p className="text-xs text-muted-foreground">
                      {c.gradedCount} baholangan · {c.studentCount} o&apos;quvchi
                    </p>
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
