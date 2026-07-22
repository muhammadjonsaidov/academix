"use client";

import { useEffect, useState } from "react";
import {
  AlertTriangle,
  ClipboardList,
  GraduationCap,
  Inbox,
  TrendingUp,
  Users,
} from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { StatTile, StatTileSkeleton } from "@/components/shared/StatTile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAdminAnalyticsStore } from "@/stores/useAdminAnalyticsStore";

export default function AdminDashboardPage() {
  const dashboard = useAdminAnalyticsStore((state) => state.dashboard);
  const fetchDashboard = useAdminAnalyticsStore((state) => state.fetchDashboard);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchDashboard()
      .catch(() => setError("Bosh sahifani yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchDashboard]);

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Bosh sahifa</h2>
          <p className="text-sm text-muted-foreground">Maktab bo&apos;yicha umumiy holat.</p>
        </div>

        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : null}

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
                label="O'quvchilar"
                value={dashboard.totalStudents}
                icon={GraduationCap}
                accentClassName="bg-role-admin-muted text-role-admin"
              />
              <StatTile
                label="O'qituvchilar"
                value={dashboard.totalTeachers}
                icon={Users}
                accentClassName="bg-role-admin-muted text-role-admin"
              />
              <StatTile
                label="Bugun faol"
                value={dashboard.activeToday}
                icon={TrendingUp}
                accentClassName="bg-role-admin-muted text-role-admin"
              />
              <StatTile
                label="Topshirish darajasi (30 kun)"
                value={`${dashboard.homeworkSubmissionRate}%`}
                icon={ClipboardList}
                accentClassName="bg-role-admin-muted text-role-admin"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <Card>
                <CardContent className="flex items-center gap-3 py-5">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-severity-high-bg text-severity-high">
                    <AlertTriangle className="size-5" strokeWidth={1.75} />
                  </span>
                  <div>
                    <p className="font-data text-2xl leading-none font-semibold">
                      {dashboard.psychologicalAlerts.high}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      Yuqori psixologik signallar
                    </p>
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="flex items-center gap-3 py-5">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-severity-medium-bg text-severity-medium">
                    <AlertTriangle className="size-5" strokeWidth={1.75} />
                  </span>
                  <div>
                    <p className="font-data text-2xl leading-none font-semibold">
                      {dashboard.psychologicalAlerts.medium}
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      O&apos;rta psixologik signallar
                    </p>
                  </div>
                </CardContent>
              </Card>
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <Card>
                <CardHeader>
                  <CardTitle>Sinflar bo&apos;yicha o&apos;zlashtirish</CardTitle>
                </CardHeader>
                <CardContent>
                  {dashboard.classProgressList.length === 0 ? (
                    <EmptyState
                      icon={Inbox}
                      title="Ma'lumot yo'q"
                      description="Sinflar bo'yicha o'zlashtirish ma'lumotlari hali mavjud emas."
                    />
                  ) : (
                    <ul className="space-y-2">
                      {dashboard.classProgressList.map((c) => (
                        <li
                          key={c.classId}
                          className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                        >
                          <span className="font-medium">{c.className}</span>
                          <span className="font-data text-muted-foreground">
                            {c.avgScore}% ({c.gradedCount} baholangan, {c.studentCount}{" "}
                            o&apos;quvchi)
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>O&apos;qituvchilar reytingi</CardTitle>
                </CardHeader>
                <CardContent>
                  {dashboard.teacherRankings.length === 0 ? (
                    <EmptyState
                      icon={Inbox}
                      title="Ma'lumot yo'q"
                      description="O'qituvchilar reytingi hali hisoblanmagan."
                    />
                  ) : (
                    <ul className="space-y-2">
                      {dashboard.teacherRankings.map((t) => (
                        <li
                          key={t.teacherId}
                          className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                        >
                          <span className="font-medium">
                            {t.firstName} {t.lastName}
                          </span>
                          <span className="font-data text-muted-foreground">
                            {t.avgGrade}/5 ({t.gradedCount})
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </CardContent>
              </Card>
            </div>
          </>
        ) : null}
      </div>
    </DashboardShell>
  );
}
