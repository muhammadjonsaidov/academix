"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  AlertTriangle,
  BarChart3,
  ClipboardList,
  GraduationCap,
  Inbox,
  School,
  TrendingUp,
  Users,
} from "lucide-react";
import { DashboardHero, HeroAction } from "@/components/shared/DashboardHero";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { ProgressBar } from "@/components/shared/ProgressBar";
import { StatTile, StatTileSkeleton } from "@/components/shared/StatTile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useAdminAnalyticsStore } from "@/stores/useAdminAnalyticsStore";
import { useAuthStore } from "@/stores/useAuthStore";

export default function AdminDashboardPage() {
  const dashboard = useAdminAnalyticsStore((state) => state.dashboard);
  const fetchDashboard = useAdminAnalyticsStore(
    (state) => state.fetchDashboard,
  );
  const firstName = useAuthStore((state) => state.user?.firstName);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchDashboard()
      .catch(() => setError("Bosh sahifani yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchDashboard]);

  const heroDescription = dashboard
    ? `${dashboard.totalStudents} o'quvchi · ${dashboard.totalTeachers} o'qituvchi · bugun ${dashboard.activeToday} faol`
    : "Maktab bo'yicha umumiy holat.";
  const isNewSchool =
    dashboard !== null &&
    dashboard.totalStudents === 0 &&
    dashboard.totalTeachers === 0;

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <DashboardHero
          title={firstName ? `Salom, ${firstName}!` : "Salom!"}
          description={heroDescription}
          actions={
            <>
              <HeroAction href="/dashboard/admin/classes" icon={School}>
                Sinflar
              </HeroAction>
              <HeroAction href="/dashboard/admin/teachers" icon={Users}>
                O&apos;qituvchilar
              </HeroAction>
              <HeroAction href="/dashboard/admin/analytics" icon={BarChart3}>
                Tahlil
              </HeroAction>
            </>
          }
        />

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        {isLoading ? (
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatTileSkeleton />
            <StatTileSkeleton />
            <StatTileSkeleton />
            <StatTileSkeleton />
          </div>
        ) : dashboard ? (
          <>
            {isNewSchool ? (
              <Card className="border-ai-soft bg-ai-soft/30">
                <CardContent className="flex flex-col gap-4 py-5 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="font-heading font-semibold">
                      Maktabni ishga tushirishga tayyormisiz?
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      Avval sinf yarating, keyin o&apos;qituvchi va
                      o&apos;quvchilarni qo&apos;shing. Shu uch qadamdan keyin
                      topshiriqlarni boshqarishni boshlaysiz.
                    </p>
                  </div>
                  <Link
                    href="/dashboard/admin/classes"
                    className={cn(
                      buttonVariants({ variant: "gradient" }),
                      "shrink-0",
                    )}
                  >
                    Birinchi sinfni yaratish
                  </Link>
                </CardContent>
              </Card>
            ) : null}
            {/* Role tiles: no accent prop — the data-role layer resolves the admin hue. */}
            <div className="stagger-rise grid grid-cols-2 gap-4 lg:grid-cols-4">
              <StatTile
                label="O'quvchilar"
                value={dashboard.totalStudents}
                icon={GraduationCap}
              />
              <StatTile
                label="O'qituvchilar"
                value={dashboard.totalTeachers}
                icon={Users}
              />
              <StatTile
                label="Bugun faol"
                value={dashboard.activeToday}
                icon={TrendingUp}
              />
              <StatTile
                label="Topshirish darajasi (30 kun)"
                value={`${dashboard.homeworkSubmissionRate}%`}
                icon={ClipboardList}
              />
            </div>

            {/* Severity tiles keep their SEMANTIC accents — never the role hue. */}
            <div className="grid gap-4 sm:grid-cols-2">
              <StatTile
                label="Yuqori psixologik signallar"
                value={dashboard.psychologicalAlerts.high}
                icon={AlertTriangle}
                accentClassName="bg-severity-high-bg text-severity-high"
              />
              <StatTile
                label="O'rta psixologik signallar"
                value={dashboard.psychologicalAlerts.medium}
                icon={AlertTriangle}
                accentClassName="bg-severity-medium-bg text-severity-medium"
              />
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <Card>
                <CardHeader>
                  <CardTitle>
                    Sinflar bo&apos;yicha o&apos;zlashtirish
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {dashboard.classProgressList.length === 0 ? (
                    <EmptyState
                      icon={Inbox}
                      title={
                        isNewSchool
                          ? "Birinchi sinfni yarating"
                          : "Ma'lumot yo'q"
                      }
                      description={
                        isNewSchool
                          ? "O'quvchilarni qo'shish uchun avval sinf yarating."
                          : "Sinflar bo'yicha o'zlashtirish ma'lumotlari hali mavjud emas."
                      }
                      action={
                        isNewSchool ? (
                          <Link
                            href="/dashboard/admin/classes"
                            className={buttonVariants({
                              variant: "outline",
                              size: "sm",
                            })}
                          >
                            Sinflarga o&apos;tish
                          </Link>
                        ) : undefined
                      }
                    />
                  ) : (
                    <ul className="space-y-2">
                      {dashboard.classProgressList.map((c) => (
                        <li
                          key={c.classId}
                          className="card-lift hover-glow space-y-1.5 rounded-md border border-border px-3 py-2.5 text-sm"
                        >
                          <div className="flex items-center justify-between gap-3">
                            <span className="truncate font-medium">
                              {c.className}
                            </span>
                            <span className="font-data shrink-0 text-muted-foreground">
                              {c.avgScore}%
                            </span>
                          </div>
                          <ProgressBar
                            value={c.avgScore}
                            tone={
                              c.avgScore >= 70
                                ? "success"
                                : c.avgScore >= 50
                                  ? "role"
                                  : "destructive"
                            }
                          />
                          <p className="text-xs text-muted-foreground">
                            {c.gradedCount} baholangan · {c.studentCount}{" "}
                            o&apos;quvchi
                          </p>
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
                      description={
                        isNewSchool
                          ? "Sinf yaratilgach, o'qituvchini taklif qiling."
                          : "O'qituvchilar reytingi hali hisoblanmagan."
                      }
                      action={
                        isNewSchool ? (
                          <Link
                            href="/dashboard/admin/teachers"
                            className={buttonVariants({
                              variant: "outline",
                              size: "sm",
                            })}
                          >
                            O&apos;qituvchi qo&apos;shish
                          </Link>
                        ) : undefined
                      }
                    />
                  ) : (
                    <ul className="space-y-2">
                      {dashboard.teacherRankings.map((t, index) => (
                        <li
                          key={t.teacherId}
                          className="card-lift hover-glow space-y-1.5 rounded-md border border-border px-3 py-2.5 text-sm"
                        >
                          <div className="flex items-center justify-between gap-3">
                            <span className="flex min-w-0 items-center gap-2.5">
                              {index < 3 ? (
                                <span
                                  className={cn(
                                    "font-data flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ring-1",
                                    index === 0
                                      ? "bg-accent-role-muted text-accent-role ring-[var(--accent-role)]/40"
                                      : "bg-muted text-muted-foreground ring-border",
                                  )}
                                >
                                  {index + 1}
                                </span>
                              ) : (
                                <span className="font-data text-xs font-semibold text-muted-foreground">
                                  #{index + 1}
                                </span>
                              )}
                              <span className="truncate font-medium">
                                {t.firstName} {t.lastName}
                              </span>
                            </span>
                            <span className="font-data shrink-0 text-muted-foreground">
                              {t.avgGrade}/5 ({t.gradedCount})
                            </span>
                          </div>
                          <ProgressBar value={t.avgGrade * 20} />
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
