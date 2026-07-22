"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ClipboardCheck, GraduationCap, School, Star, TrendingUp } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTeacherAnalyticsStore } from "@/stores/useTeacherAnalyticsStore";

export default function TeacherDashboardPage() {
  const dashboard = useTeacherAnalyticsStore((state) => state.dashboard);
  const fetchDashboard = useTeacherAnalyticsStore((state) => state.fetchDashboard);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Bosh sahifani yuklab bo'lmadi."));
  }, [fetchDashboard]);

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-4 font-heading text-lg font-semibold">Bosh sahifa</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {!dashboard && !error ? (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : null}

      {dashboard ? (
        <>
          <div className="stagger-rise mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard
              icon={ClipboardCheck}
              value={dashboard.pendingSubmissions}
              label="Kutilayotgan ishlar"
            />
            <StatCard icon={GraduationCap} value={dashboard.gradedToday} label="Bugun baholandi" />
            <StatCard
              icon={Star}
              value={`${dashboard.myRating.score}/5`}
              label={`Reyting (${dashboard.myRating.trend})`}
            />
            <StatCard icon={School} value={dashboard.myClasses.length} label="Sinflarim" />
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
                <EmptyState icon={School} title="Ma'lumot yo'q" description="Sinflaringiz bo'yicha o'zlashtirish ma'lumotlari hali mavjud emas." />
              ) : null}
            </CardContent>
          </Card>
        </>
      ) : null}
    </DashboardShell>
  );
}

function StatCard({
  icon: Icon,
  value,
  label,
}: {
  icon: React.ComponentType<{ className?: string; strokeWidth?: number }>;
  value: string | number;
  label: string;
}) {
  return (
    <Card>
      <CardContent className="flex items-center gap-3">
        <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-role-teacher-muted text-role-teacher">
          <Icon className="size-4.5" strokeWidth={1.75} />
        </span>
        <div>
          <p className="font-data text-xl font-semibold leading-tight">{value}</p>
          <p className="text-xs text-muted-foreground">{label}</p>
        </div>
      </CardContent>
    </Card>
  );
}
