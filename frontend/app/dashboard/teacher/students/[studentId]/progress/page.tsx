"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { useTeacherAnalyticsStore } from "@/stores/useTeacherAnalyticsStore";

export default function TeacherStudentProgressPage() {
  const params = useParams<{ studentId: string }>();
  const progress = useTeacherAnalyticsStore((state) => state.studentProgress);
  const fetchStudentProgress = useTeacherAnalyticsStore((state) => state.fetchStudentProgress);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStudentProgress(params.studentId).catch(() => setError("Progressni yuklab bo'lmadi."));
  }, [params.studentId, fetchStudentProgress]);

  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {progress ? (
        <>
          <h2 className="mb-4 text-lg font-semibold">
            {progress.student.firstName} {progress.student.lastName}
          </h2>

          <div className="mb-6">
            <h3 className="mb-2 text-sm font-semibold">Fanlar bo&apos;yicha</h3>
            <ul className="space-y-2">
              {progress.subjectStats.map((s) => (
                <li key={s.subject} className="rounded-md border border-border p-3 text-sm">
                  <div className="flex justify-between">
                    <span>{s.subject}</span>
                    <span className="text-muted-foreground">
                      {s.averageScore}% ({s.trend})
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Topshirish darajasi: {s.submissionRate}%
                  </p>
                </li>
              ))}
              {progress.subjectStats.length === 0 ? (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              ) : null}
            </ul>
          </div>

          <div className="mb-6">
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

          <div>
            <h3 className="mb-2 text-sm font-semibold">So&apos;nggi topshirilgan ishlar</h3>
            <ul className="space-y-2">
              {progress.recentSubmissions.map((s) => (
                <li
                  key={s.submissionId}
                  className="flex justify-between rounded-md border border-border p-3 text-sm"
                >
                  <span>{s.status}</span>
                  <span className="text-muted-foreground">
                    {new Date(s.submittedAt).toLocaleDateString()}
                    {s.isLate ? " (kech)" : ""}
                  </span>
                </li>
              ))}
              {progress.recentSubmissions.length === 0 ? (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              ) : null}
            </ul>
          </div>
        </>
      ) : null}
    </DashboardShell>
  );
}
