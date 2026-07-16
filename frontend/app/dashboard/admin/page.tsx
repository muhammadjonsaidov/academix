"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { useAdminAnalyticsStore } from "@/stores/useAdminAnalyticsStore";

export default function AdminDashboardPage() {
  const dashboard = useAdminAnalyticsStore((state) => state.dashboard);
  const fetchDashboard = useAdminAnalyticsStore((state) => state.fetchDashboard);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Bosh sahifani yuklab bo'lmadi."));
  }, [fetchDashboard]);

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {dashboard ? (
        <>
          <div className="mb-6 grid grid-cols-4 gap-4">
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.totalStudents}</p>
              <p className="text-sm text-muted-foreground">O&apos;quvchilar</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.totalTeachers}</p>
              <p className="text-sm text-muted-foreground">O&apos;qituvchilar</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.activeToday}</p>
              <p className="text-sm text-muted-foreground">Bugun faol</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.homeworkSubmissionRate}%</p>
              <p className="text-sm text-muted-foreground">Topshirish darajasi (30 kun)</p>
            </div>
          </div>

          <div className="mb-6 grid grid-cols-2 gap-4">
            <div className="rounded-md border border-border p-4">
              <p className="text-lg font-semibold text-destructive">
                {dashboard.psychologicalAlerts.high}
              </p>
              <p className="text-sm text-muted-foreground">Yuqori psixologik signallar</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-lg font-semibold text-amber-600">
                {dashboard.psychologicalAlerts.medium}
              </p>
              <p className="text-sm text-muted-foreground">O&apos;rta psixologik signallar</p>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-6">
            <div>
              <h3 className="mb-2 text-sm font-semibold">Sinflar bo&apos;yicha o&apos;zlashtirish</h3>
              <ul className="space-y-2">
                {dashboard.classProgressList.map((c) => (
                  <li
                    key={c.classId}
                    className="flex justify-between rounded-md border border-border p-3 text-sm"
                  >
                    <span>{c.className}</span>
                    <span className="text-muted-foreground">
                      {c.avgScore}% ({c.gradedCount} baholangan, {c.studentCount} o&apos;quvchi)
                    </span>
                  </li>
                ))}
                {dashboard.classProgressList.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : null}
              </ul>
            </div>

            <div>
              <h3 className="mb-2 text-sm font-semibold">O&apos;qituvchilar reytingi</h3>
              <ul className="space-y-2">
                {dashboard.teacherRankings.map((t) => (
                  <li
                    key={t.teacherId}
                    className="flex justify-between rounded-md border border-border p-3 text-sm"
                  >
                    <span>
                      {t.firstName} {t.lastName}
                    </span>
                    <span className="text-muted-foreground">
                      {t.avgGrade}/5 ({t.gradedCount})
                    </span>
                  </li>
                ))}
                {dashboard.teacherRankings.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : null}
              </ul>
            </div>
          </div>
        </>
      ) : null}
    </DashboardShell>
  );
}
