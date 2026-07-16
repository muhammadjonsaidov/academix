"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useStudentStore } from "@/stores/useStudentStore";

export default function StudentDashboardPage() {
  const dashboard = useStudentStore((state) => state.dashboard);
  const fetchDashboard = useStudentStore((state) => state.fetchDashboard);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Ma'lumotlarni yuklab bo'lmadi."));
  }, [fetchDashboard]);

  if (error) {
    return (
      <DashboardShell role="STUDENT">
        <StudentNav />
        <p className="text-sm text-destructive">{error}</p>
      </DashboardShell>
    );
  }

  if (!dashboard) {
    return (
      <DashboardShell role="STUDENT">
        <StudentNav />
        <p>Yuklanmoqda...</p>
      </DashboardShell>
    );
  }

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">Salom, {dashboard.profile.firstName}!</h2>

      <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <div className="rounded-md border border-border p-4">
          <p className="text-2xl font-semibold">{dashboard.profile.totalXp}</p>
          <p className="text-sm text-muted-foreground">Jami XP</p>
        </div>
        <div className="rounded-md border border-border p-4">
          <p className="text-2xl font-semibold">{dashboard.profile.currentStreak}</p>
          <p className="text-sm text-muted-foreground">Joriy seriya (kun)</p>
        </div>
        <div className="rounded-md border border-border p-4">
          <p className="text-2xl font-semibold">{dashboard.profile.maxStreak}</p>
          <p className="text-sm text-muted-foreground">Eng uzun seriya</p>
        </div>
        <div className="rounded-md border border-border p-4">
          <p className="text-2xl font-semibold">{dashboard.xpToNextBadge}</p>
          <p className="text-sm text-muted-foreground">Keyingi yutuqqacha XP</p>
        </div>
      </div>

      <div className="mb-6">
        <div className="mb-2 flex items-center justify-between">
          <h3 className="font-medium">Yutuqlar</h3>
          <Link href="/dashboard/student/badges" className="text-sm underline">
            Barchasi
          </Link>
        </div>
        {dashboard.badges.length > 0 ? (
          <div className="flex flex-wrap gap-3">
            {dashboard.badges.map((badge) => (
              <div
                key={badge.id}
                title={badge.description}
                className="flex items-center gap-2 rounded-md border border-border px-3 py-2 text-sm"
              >
                <span>{badge.icon}</span>
                <span>{badge.name}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">Hozircha yutuqlar yo&apos;q.</p>
        )}
      </div>

      <div className="mb-6">
        <h3 className="mb-2 font-medium">Topshirilishi kerak</h3>
        <ul className="space-y-2">
          {dashboard.pendingHomework.map((hw) => (
            <li
              key={hw.assignmentId}
              className="flex items-center justify-between rounded-md border border-border p-3 text-sm"
            >
              <span>
                {hw.title} ({hw.subject})
              </span>
              <span className="text-muted-foreground">
                {new Date(hw.deadlineAt).toLocaleString()}
              </span>
            </li>
          ))}
          {dashboard.pendingHomework.length === 0 ? (
            <p className="text-sm text-muted-foreground">Hozircha vazifalar yo&apos;q.</p>
          ) : null}
        </ul>
      </div>

      <div>
        <h3 className="mb-2 font-medium">So&apos;nggi baholar</h3>
        <ul className="space-y-2">
          {dashboard.recentGrades.map((grade) => (
            <li
              key={grade.submissionId}
              className="flex items-center justify-between rounded-md border border-border p-3 text-sm"
            >
              <span>
                {grade.score} ball ({grade.fivePointGrade})
              </span>
              <span className="text-muted-foreground">
                {new Date(grade.gradedAt).toLocaleString()}
              </span>
            </li>
          ))}
          {dashboard.recentGrades.length === 0 ? (
            <p className="text-sm text-muted-foreground">Hozircha baholar yo&apos;q.</p>
          ) : null}
        </ul>
      </div>
    </DashboardShell>
  );
}
