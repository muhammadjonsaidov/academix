"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BookOpen, Flame, GraduationCap, Sparkles, Star, Trophy } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { StatCard } from "@/components/student/StatCard";
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
        <p className="text-sm text-destructive">{error}</p>
      </DashboardShell>
    );
  }

  if (!dashboard) {
    return (
      <DashboardShell role="STUDENT">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20" />
          ))}
        </div>
      </DashboardShell>
    );
  }

  return (
    <DashboardShell role="STUDENT">
      <h2 className="mb-4 font-heading text-lg font-semibold">
        Salom, {dashboard.profile.firstName}! 👋
      </h2>

      <div className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard icon={Star} value={dashboard.profile.totalXp} label="Jami XP" />
        <StatCard icon={Flame} value={dashboard.profile.currentStreak} label="Joriy seriya (kun)" />
        <StatCard icon={Trophy} value={dashboard.profile.maxStreak} label="Eng uzun seriya" />
        <StatCard icon={Sparkles} value={dashboard.xpToNextBadge} label="Keyingi yutuqqacha XP" />
      </div>

      <Card className="mb-6">
        <CardHeader className="flex-row items-center justify-between">
          <CardTitle>Yutuqlar</CardTitle>
          <Link
            href="/dashboard/student/badges"
            className="text-sm font-medium text-role-student hover:underline"
          >
            Barchasi
          </Link>
        </CardHeader>
        <CardContent>
          {dashboard.badges.length > 0 ? (
            <div className="flex flex-wrap gap-3">
              {dashboard.badges.map((badge) => (
                <div
                  key={badge.id}
                  title={badge.description}
                  className="flex items-center gap-2 rounded-full border border-role-student/30 bg-role-student-muted px-3 py-1.5 text-sm text-role-student"
                >
                  <span className="text-base leading-none">{badge.icon}</span>
                  <span className="font-medium">{badge.name}</span>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={Trophy}
              title="Hozircha yutuqlar yo'q"
              description="Vazifalarni bajarib, birinchi yutug'ingizni qo'lga kiriting."
            />
          )}
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Topshirilishi kerak</CardTitle>
        </CardHeader>
        <CardContent>
          {dashboard.pendingHomework.length > 0 ? (
            <ul className="space-y-2">
              {dashboard.pendingHomework.map((hw) => (
                <li
                  key={hw.assignmentId}
                  className="flex items-center justify-between gap-3 rounded-md border border-border p-3 text-sm"
                >
                  <span className="min-w-0 truncate">
                    {hw.title} <span className="text-muted-foreground">({hw.subject})</span>
                  </span>
                  <span className="font-data shrink-0 text-muted-foreground">
                    {new Date(hw.deadlineAt).toLocaleString()}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState
              icon={BookOpen}
              title="Hozircha vazifalar yo'q"
              description="Yangi uy vazifalari shu yerda ko'rinadi."
            />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>So&apos;nggi baholar</CardTitle>
        </CardHeader>
        <CardContent>
          {dashboard.recentGrades.length > 0 ? (
            <ul className="space-y-2">
              {dashboard.recentGrades.map((grade) => (
                <li
                  key={grade.submissionId}
                  className="rail-verified flex items-center justify-between gap-3 rounded-md bg-muted/40 p-3 pl-4 text-sm"
                >
                  <span className="font-data font-medium">
                    {grade.score} ball ({grade.fivePointGrade})
                  </span>
                  <span className="text-muted-foreground">
                    {new Date(grade.gradedAt).toLocaleString()}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState
              icon={GraduationCap}
              title="Hozircha baholar yo'q"
              description="Baholangan ishlaringiz shu yerda ko'rinadi."
            />
          )}
        </CardContent>
      </Card>
    </DashboardShell>
  );
}
