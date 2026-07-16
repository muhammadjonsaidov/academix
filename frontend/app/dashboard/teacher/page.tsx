"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
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
      <TeacherNav />

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {dashboard ? (
        <>
          <div className="mb-6 grid grid-cols-4 gap-4">
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.pendingSubmissions}</p>
              <p className="text-sm text-muted-foreground">Kutilayotgan ishlar</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.gradedToday}</p>
              <p className="text-sm text-muted-foreground">Bugun baholandi</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.myRating.score}/5</p>
              <p className="text-sm text-muted-foreground">Reyting ({dashboard.myRating.trend})</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.myClasses.length}</p>
              <p className="text-sm text-muted-foreground">Sinflarim</p>
            </div>
          </div>

          <h3 className="mb-2 text-sm font-semibold">Sinflar bo&apos;yicha o&apos;zlashtirish</h3>
          <ul className="space-y-2">
            {dashboard.classProgressSummary.map((c) => (
              <li key={c.classId} className="rounded-md border border-border p-3 text-sm">
                <Link
                  href={`/dashboard/teacher/classes/${c.classId}/analytics`}
                  className="flex justify-between hover:underline"
                >
                  <span>{c.className}</span>
                  <span className="text-muted-foreground">
                    {c.avgScore}% ({c.gradedCount} baholangan, {c.studentCount} o&apos;quvchi)
                  </span>
                </Link>
              </li>
            ))}
            {dashboard.classProgressSummary.length === 0 ? (
              <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
            ) : null}
          </ul>
        </>
      ) : null}
    </DashboardShell>
  );
}
