"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useStudentStore } from "@/stores/useStudentStore";

export default function StudentProgressPage() {
  const progress = useStudentStore((state) => state.progress);
  const fetchProgress = useStudentStore((state) => state.fetchProgress);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchProgress().catch(() => setError("Progressni yuklab bo'lmadi."));
  }, [fetchProgress]);

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">Mening progressim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {progress ? (
        <>
          <div className="mb-6 grid grid-cols-2 gap-4">
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{progress.myGrowth.thisMonth.avgScore}%</p>
              <p className="text-sm text-muted-foreground">
                Shu oy (o&apos;tgan oy: {progress.myGrowth.lastMonth.avgScore}%, {progress.myGrowth.growth})
              </p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{progress.badges.length}</p>
              <p className="text-sm text-muted-foreground">Yutuqlar</p>
            </div>
          </div>

          <div className="mb-6">
            <h3 className="mb-2 text-sm font-semibold">Fanlar bo&apos;yicha</h3>
            <ul className="space-y-2">
              {progress.subjectStats.map((s) => (
                <li key={s.subject} className="rounded-md border border-border p-3 text-sm">
                  <div className="flex justify-between">
                    <span>{s.subject}</span>
                    <span className="text-muted-foreground">
                      {s.currentAvg}% ({s.trend})
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    O&apos;tgan oy: {s.previousMonthAvg}% — Topshirish darajasi: {Math.round(s.submissionRate * 100)}%
                  </p>
                </li>
              ))}
              {progress.subjectStats.length === 0 ? (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              ) : null}
            </ul>
          </div>

          <div>
            <h3 className="mb-2 text-sm font-semibold">XP tarixi</h3>
            <ul className="space-y-2">
              {progress.xpHistory.slice(0, 10).map((x, i) => (
                <li
                  key={i}
                  className="flex justify-between rounded-md border border-border p-3 text-sm"
                >
                  <span>{x.reason}</span>
                  <span className="text-muted-foreground">
                    +{x.xp} XP — {new Date(x.date).toLocaleDateString()}
                  </span>
                </li>
              ))}
              {progress.xpHistory.length === 0 ? (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              ) : null}
            </ul>
          </div>
        </>
      ) : null}
    </DashboardShell>
  );
}
