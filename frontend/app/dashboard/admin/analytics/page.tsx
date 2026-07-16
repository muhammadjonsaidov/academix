"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { Button } from "@/components/ui/button";
import { useAdminAnalyticsStore } from "@/stores/useAdminAnalyticsStore";

type Period = "monthly" | "semester";

export default function AdminAnalyticsPage() {
  const classesComparison = useAdminAnalyticsStore((state) => state.classesComparison);
  const teachersRanking = useAdminAnalyticsStore((state) => state.teachersRanking);
  const schoolProgress = useAdminAnalyticsStore((state) => state.schoolProgress);
  const fetchClassesComparison = useAdminAnalyticsStore((state) => state.fetchClassesComparison);
  const fetchTeachersRanking = useAdminAnalyticsStore((state) => state.fetchTeachersRanking);
  const fetchSchoolProgress = useAdminAnalyticsStore((state) => state.fetchSchoolProgress);
  const [period, setPeriod] = useState<Period>("monthly");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      fetchClassesComparison(period),
      fetchTeachersRanking(),
      fetchSchoolProgress(period),
    ]).catch(() => setError("Tahlil ma'lumotlarini yuklab bo'lmadi."));
  }, [period, fetchClassesComparison, fetchTeachersRanking, fetchSchoolProgress]);

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <div className="mb-4 flex items-center gap-2">
        <h2 className="text-lg font-semibold">Tahlil</h2>
        <div className="ml-auto flex gap-2">
          <Button
            variant={period === "monthly" ? "default" : "outline"}
            size="sm"
            onClick={() => setPeriod("monthly")}
          >
            Oylik
          </Button>
          <Button
            variant={period === "semester" ? "default" : "outline"}
            size="sm"
            onClick={() => setPeriod("semester")}
          >
            Semestrlik
          </Button>
        </div>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <div className="grid grid-cols-2 gap-6">
        <div>
          <h3 className="mb-2 text-sm font-semibold">Sinflar taqqoslash</h3>
          <ul className="space-y-2">
            {classesComparison.map((c) => (
              <li
                key={c.classId}
                className="flex justify-between rounded-md border border-border p-3 text-sm"
              >
                <span>{c.className}</span>
                <span className="text-muted-foreground">
                  {c.avgScore}% ({c.gradedCount} baholangan)
                </span>
              </li>
            ))}
            {classesComparison.length === 0 ? (
              <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
            ) : null}
          </ul>
        </div>

        <div>
          <h3 className="mb-2 text-sm font-semibold">O&apos;qituvchilar reytingi</h3>
          <ul className="space-y-2">
            {teachersRanking.map((t) => (
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
            {teachersRanking.length === 0 ? (
              <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
            ) : null}
          </ul>
        </div>
      </div>

      <div className="mt-6">
        <h3 className="mb-2 text-sm font-semibold">Maktab bo&apos;yicha dinamika</h3>
        <ul className="space-y-2">
          {schoolProgress.map((p) => (
            <li
              key={p.periodStart}
              className="flex justify-between rounded-md border border-border p-3 text-sm"
            >
              <span>{new Date(p.periodStart).toLocaleDateString()}</span>
              <span className="text-muted-foreground">
                {p.avgScore}% ({p.gradedCount} baholangan)
              </span>
            </li>
          ))}
          {schoolProgress.length === 0 ? (
            <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
          ) : null}
        </ul>
      </div>
    </DashboardShell>
  );
}
