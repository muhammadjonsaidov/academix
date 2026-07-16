"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
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
      <TeacherNav />
      <h2 className="mb-4 text-lg font-semibold">Sinf tahlili</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {analytics ? (
        <>
          <div className="mb-6 rounded-md border border-border p-4">
            <p className="text-2xl font-semibold">{analytics.classAverage}%</p>
            <p className="text-sm text-muted-foreground">Sinf o&apos;rtacha bahosi</p>
          </div>

          <div className="mb-6 grid grid-cols-2 gap-6">
            <div>
              <h3 className="mb-2 text-sm font-semibold">Eng yaxshi o&apos;quvchilar</h3>
              <ul className="space-y-2">
                {analytics.topStudents.map((s) => (
                  <li
                    key={s.studentId}
                    className="rounded-md border border-border p-3 text-sm"
                  >
                    <Link
                      href={`/dashboard/teacher/students/${s.studentId}/progress`}
                      className="flex justify-between hover:underline"
                    >
                      <span>
                        {s.firstName} {s.lastName}
                      </span>
                      <span className="text-muted-foreground">{s.avgScore}%</span>
                    </Link>
                  </li>
                ))}
                {analytics.topStudents.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : null}
              </ul>
            </div>
            <div>
              <h3 className="mb-2 text-sm font-semibold">Yordam kerak bo&apos;lgan o&apos;quvchilar</h3>
              <ul className="space-y-2">
                {analytics.bottomStudents.map((s) => (
                  <li
                    key={s.studentId}
                    className="rounded-md border border-border p-3 text-sm"
                  >
                    <Link
                      href={`/dashboard/teacher/students/${s.studentId}/progress`}
                      className="flex justify-between hover:underline"
                    >
                      <span>
                        {s.firstName} {s.lastName}
                      </span>
                      <span className="text-muted-foreground">{s.avgScore}%</span>
                    </Link>
                  </li>
                ))}
                {analytics.bottomStudents.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : null}
              </ul>
            </div>
          </div>

          <div className="mb-6">
            <h3 className="mb-2 text-sm font-semibold">Zaif fanlar</h3>
            <p className="text-sm text-muted-foreground">
              {analytics.subjectWeakAreas.length > 0
                ? analytics.subjectWeakAreas.join(", ")
                : "Ma'lumot yo'q."}
            </p>
          </div>

          <div>
            <h3 className="mb-2 text-sm font-semibold">Fanlar bo&apos;yicha topshirish darajasi</h3>
            <ul className="space-y-2">
              {analytics.submissionRateBySubject.map((s) => (
                <li
                  key={s.subject}
                  className="flex justify-between rounded-md border border-border p-3 text-sm"
                >
                  <span>{s.subject}</span>
                  <span className="text-muted-foreground">
                    {s.averageScore}% o&apos;rtacha, {s.submissionRate}% topshirilgan
                  </span>
                </li>
              ))}
              {analytics.submissionRateBySubject.length === 0 ? (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              ) : null}
            </ul>
          </div>
        </>
      ) : null}
    </DashboardShell>
  );
}
