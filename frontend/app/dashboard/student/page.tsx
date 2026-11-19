"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  BookOpen,
  Flame,
  GraduationCap,
  MessageSquareText,
  Sparkles,
  Star,
  Trophy,
} from "lucide-react";
import { DashboardHero, HeroAction } from "@/components/shared/DashboardHero";
import { DashboardShell } from "@/components/shell/DashboardShell";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StampBadge } from "@/components/ui/stamp-badge";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { GradeMark } from "@/components/shared/GradeMark";
import { ProgressBar } from "@/components/shared/ProgressBar";
import { StatCard } from "@/components/student/StatCard";
import { useStudentStore } from "@/stores/useStudentStore";
import { useAuthStore } from "@/stores/useAuthStore";
import { deadlineLabel, formatDate } from "@/lib/format";

export default function StudentDashboardPage() {
  const dashboard = useStudentStore((state) => state.dashboard);
  const fetchDashboard = useStudentStore((state) => state.fetchDashboard);
  const firstName = useAuthStore((state) => state.user?.firstName);
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

  // Progress toward the next badge: xpToNextBadge is what's still missing, so the
  // full ladder is totalXp + xpToNextBadge. Only rendered when a next badge exists.
  const xpLadderTotal = dashboard.profile.totalXp + dashboard.xpToNextBadge;
  const xpProgress =
    xpLadderTotal > 0 ? (dashboard.profile.totalXp / xpLadderTotal) * 100 : 0;

  return (
    <DashboardShell role="STUDENT">
      <div className="space-y-6">
        <DashboardHero
          title={firstName ? `Salom, ${firstName}` : "Salom"}
          description={
            dashboard.pendingHomework.length > 0
              ? `${dashboard.pendingHomework.length} ta vazifani topshirish kerak. Seriyangiz ${dashboard.profile.currentStreak} kun — uzmang.`
              : `Hamma vazifalar topshirilgan. Seriyangiz ${dashboard.profile.currentStreak} kun.`
          }
          actions={
            <>
              <HeroAction href="/dashboard/student/homework" icon={BookOpen}>
                Vazifalar
              </HeroAction>
              <HeroAction href="/dashboard/student/ai-chat" icon={MessageSquareText}>
                AI Tutor
              </HeroAction>
              <HeroAction href="/dashboard/student/badges" icon={Trophy}>
                Yutuqlar
              </HeroAction>
            </>
          }
        />

        <div className="stagger-rise grid grid-cols-2 gap-4 sm:grid-cols-4">
          <StatCard icon={Star} value={dashboard.profile.totalXp} label="Jami XP" />
          <StatCard
            icon={Flame}
            value={dashboard.profile.currentStreak}
            label="Joriy seriya (kun)"
          />
          <StatCard icon={Trophy} value={dashboard.profile.maxStreak} label="Eng uzun seriya" />
          <StatCard
            icon={BookOpen}
            value={dashboard.pendingHomework.length}
            label="Topshirilishi kerak"
          />
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center justify-between gap-2">
              Topshirish kerak
              <Link href="/dashboard/student/homework" className="text-sm font-normal text-muted-foreground hover:text-foreground hover:underline">
                Hammasi
              </Link>
            </CardTitle>
          </CardHeader>
          <CardContent>
            {dashboard.pendingHomework.length > 0 ? (
              <ul className="space-y-2">
                {dashboard.pendingHomework.map((hw) => (
                  <li
                    key={hw.assignmentId}
                    className="flex items-center justify-between gap-3 border-b border-border py-2 text-sm last:border-0"
                  >
                    <span className="flex min-w-0 items-center gap-2">
                      <span className="min-w-0 truncate">
                        {hw.title} <span className="text-muted-foreground">({hw.subject})</span>
                      </span>
                      {hw.isLate ? (
                        <Badge variant="destructive" className="shrink-0">
                          Kechikkan
                        </Badge>
                      ) : null}
                    </span>
                    <span className="shrink-0 text-muted-foreground">{deadlineLabel(hw.deadlineAt)}</span>
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
                    className="flex items-center justify-between gap-3 border-b border-border py-2 text-sm last:border-0"
                  >
                    <span className="flex min-w-0 items-center gap-3">
                      <GradeMark grade={grade.fivePointGrade} size="sm" />
                      <span className="font-data font-medium">{grade.score} ball</span>
                    </span>
                    <span className="shrink-0 text-xs text-muted-foreground">{formatDate(grade.gradedAt)}</span>
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
        </div>

        {dashboard.xpToNextBadge > 0 ? (
          <Card>
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Sparkles className="size-4 text-role-student" strokeWidth={1.75} />
                Keyingi yutuq sari
              </CardTitle>
              <span className="font-data text-sm font-medium text-role-student">
                {dashboard.xpToNextBadge} XP qoldi
              </span>
            </CardHeader>
            <CardContent>
              <ProgressBar value={xpProgress} />
              <p className="mt-2 text-xs text-muted-foreground">
                Jami {dashboard.profile.totalXp} XP to&apos;pladingiz. Keyingi yutuqni ochish uchun
                yana {dashboard.xpToNextBadge} XP kerak.
              </p>
            </CardContent>
          </Card>
        ) : null}

        <Card>
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
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {dashboard.badges.map((badge) => (
                  <div
                    key={badge.id}
                    title={badge.description}
                    className="flex items-center gap-3 rounded-lg border border-border bg-card p-3"
                  >
                    <StampBadge variant="chalk-green" size="md" className="shrink-0">
                      <span className="text-lg leading-none">{badge.icon}</span>
                    </StampBadge>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold">{badge.name}</p>
                      <p className="truncate text-xs text-muted-foreground">{badge.description}</p>
                    </div>
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

      </div>
    </DashboardShell>
  );
}
