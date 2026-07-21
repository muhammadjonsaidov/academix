"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { AlertTriangle, ArrowDown, ArrowUp, BarChart3 } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTeacherAnalyticsStore } from "@/stores/useTeacherAnalyticsStore";

export default function TeacherClassAnalyticsPage() {
  const params = useParams<{ classId: string }>();
  const analytics = useTeacherAnalyticsStore((state) => state.classAnalytics);
  const fetchClassAnalytics = useTeacherAnalyticsStore((state) => state.fetchClassAnalytics);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchClassAnalytics(params.classId).catch(() => setError("Tahlilni yuklab bo'lmadi."));
  }, [params.classId, fetchClassAnalytics]);

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-4 font-heading text-lg font-semibold">Sinf tahlili</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {!analytics && !error ? (
        <div className="space-y-4">
          <Skeleton className="h-24 w-48" />
          <div className="grid grid-cols-2 gap-6">
            <Skeleton className="h-40" />
            <Skeleton className="h-40" />
          </div>
        </div>
      ) : null}

      {analytics ? (
        <>
          <Card className="mb-6 w-fit">
            <CardContent className="flex items-center gap-3">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-role-teacher-muted text-role-teacher">
                <BarChart3 className="size-4.5" strokeWidth={1.75} />
              </span>
              <div>
                <p className="font-data text-2xl font-semibold leading-tight">{analytics.classAverage}%</p>
                <p className="text-xs text-muted-foreground">Sinf o&apos;rtacha bahosi</p>
              </div>
            </CardContent>
          </Card>

          <div className="mb-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <ArrowUp className="size-4 text-success" strokeWidth={1.75} />
                  Eng yaxshi o&apos;quvchilar
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {analytics.topStudents.map((s) => (
                  <Link
                    key={s.studentId}
                    href={`/dashboard/teacher/students/${s.studentId}/progress`}
                    className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm transition-colors hover:bg-muted/60"
                  >
                    <span>
                      {s.firstName} {s.lastName}
                    </span>
                    <span className="font-data text-muted-foreground">{s.avgScore}%</span>
                  </Link>
                ))}
                {analytics.topStudents.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : null}
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <ArrowDown className="size-4 text-severity-medium" strokeWidth={1.75} />
                  Yordam kerak bo&apos;lgan o&apos;quvchilar
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {analytics.bottomStudents.map((s) => (
                  <Link
                    key={s.studentId}
                    href={`/dashboard/teacher/students/${s.studentId}/progress`}
                    className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm transition-colors hover:bg-muted/60"
                  >
                    <span>
                      {s.firstName} {s.lastName}
                    </span>
                    <span className="font-data text-muted-foreground">{s.avgScore}%</span>
                  </Link>
                ))}
                {analytics.bottomStudents.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : null}
              </CardContent>
            </Card>
          </div>

          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="size-4 text-severity-medium" strokeWidth={1.75} />
                Zaif fanlar
              </CardTitle>
            </CardHeader>
            <CardContent>
              {analytics.subjectWeakAreas.length > 0 ? (
                <p className="text-sm text-muted-foreground">{analytics.subjectWeakAreas.join(", ")}</p>
              ) : (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Fanlar bo&apos;yicha topshirish darajasi</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {analytics.submissionRateBySubject.map((s) => (
                <div
                  key={s.subject}
                  className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                >
                  <span>{s.subject}</span>
                  <span className="font-data text-muted-foreground">
                    {s.averageScore}% o&apos;rtacha &middot; {s.submissionRate}% topshirilgan
                  </span>
                </div>
              ))}
              {analytics.submissionRateBySubject.length === 0 ? (
                <EmptyState
                  icon={BarChart3}
                  title="Ma'lumot yo'q"
                  description="Bu sinf uchun fanlar bo'yicha topshirish ma'lumotlari hali mavjud emas."
                />
              ) : null}
            </CardContent>
          </Card>
        </>
      ) : null}
    </DashboardShell>
  );
}
