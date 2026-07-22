"use client";

import { useEffect, useState } from "react";
import { BarChart3, Bot, Inbox, TrendingUp, Users } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminAnalyticsStore } from "@/stores/useAdminAnalyticsStore";

type Period = "monthly" | "quarter";

export default function AdminAnalyticsPage() {
  const [period, setPeriod] = useState<Period>("monthly");

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-heading text-xl font-semibold">Tahlil</h2>
            <p className="text-sm text-muted-foreground">
              Sinflar, o&apos;qituvchilar va AI foydalanish bo&apos;yicha chuqur tahlil.
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              variant={period === "monthly" ? "default" : "outline"}
              size="sm"
              onClick={() => setPeriod("monthly")}
            >
              Oylik
            </Button>
            <Button
              variant={period === "quarter" ? "default" : "outline"}
              size="sm"
              onClick={() => setPeriod("quarter")}
            >
              Choraklik
            </Button>
          </div>
        </div>

        {/* Keyed by period so the loading state resets on a fresh mount (isLoading starts
            true via useState) instead of a synchronous setState(true) inside the effect. */}
        <AnalyticsBody key={period} period={period} />
      </div>
    </DashboardShell>
  );
}

function AnalyticsBody({ period }: { period: Period }) {
  const classesComparison = useAdminAnalyticsStore((state) => state.classesComparison);
  const teachersRanking = useAdminAnalyticsStore((state) => state.teachersRanking);
  const schoolProgress = useAdminAnalyticsStore((state) => state.schoolProgress);
  const aiUsage = useAdminAnalyticsStore((state) => state.aiUsage);
  const fetchClassesComparison = useAdminAnalyticsStore((state) => state.fetchClassesComparison);
  const fetchTeachersRanking = useAdminAnalyticsStore((state) => state.fetchTeachersRanking);
  const fetchSchoolProgress = useAdminAnalyticsStore((state) => state.fetchSchoolProgress);
  const fetchAiUsage = useAdminAnalyticsStore((state) => state.fetchAiUsage);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetchClassesComparison(period),
      fetchTeachersRanking(),
      fetchSchoolProgress(period),
      fetchAiUsage(period),
    ])
      .catch(() => setError("Tahlil ma'lumotlarini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [period, fetchClassesComparison, fetchTeachersRanking, fetchSchoolProgress, fetchAiUsage]);

  return (
    <>
      {error ? <p className="text-sm text-destructive">{error}</p> : null}

      <div className="space-y-6">
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="size-4" strokeWidth={1.75} />
                Sinflar taqqoslash
              </CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <ListSkeleton />
              ) : classesComparison.length === 0 ? (
                <EmptyState icon={Inbox} title="Ma'lumot yo'q" description="Ushbu davr uchun sinflar bo'yicha ma'lumot topilmadi." />
              ) : (
                <ul className="space-y-2">
                  {classesComparison.map((c) => (
                    <li
                      key={c.classId}
                      className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                    >
                      <span className="font-medium">{c.className}</span>
                      <span className="font-data text-muted-foreground">
                        {c.avgScore}% ({c.gradedCount} baholangan)
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Users className="size-4" strokeWidth={1.75} />
                O&apos;qituvchilar reytingi
              </CardTitle>
            </CardHeader>
            <CardContent>
              {isLoading ? (
                <ListSkeleton />
              ) : teachersRanking.length === 0 ? (
                <EmptyState icon={Inbox} title="Ma'lumot yo'q" description="O'qituvchilar reytingi hali hisoblanmagan." />
              ) : (
                <ul className="space-y-2">
                  {teachersRanking.map((t) => (
                    <li
                      key={t.teacherId}
                      className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                    >
                      <span className="font-medium">
                        {t.firstName} {t.lastName}
                      </span>
                      <span className="font-data text-muted-foreground">
                        {t.avgGrade}/5 ({t.gradedCount})
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="size-4" strokeWidth={1.75} />
              Maktab bo&apos;yicha dinamika
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <ListSkeleton />
            ) : schoolProgress.length === 0 ? (
              <EmptyState icon={Inbox} title="Ma'lumot yo'q" description="Ushbu davr uchun dinamika ma'lumoti topilmadi." />
            ) : (
              <ul className="space-y-2">
                {schoolProgress.map((p) => (
                  <li
                    key={p.periodStart}
                    className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                  >
                    <span className="font-data">{new Date(p.periodStart).toLocaleDateString()}</span>
                    <span className="font-data text-muted-foreground">
                      {p.avgScore}% ({p.gradedCount} baholangan)
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bot className="size-4" strokeWidth={1.75} />
              AI foydalanish (faqat baholash chaqiruvlari)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-6 sm:grid-cols-3">
              <div>
                <p className="mb-2 text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Sinflar bo&apos;yicha
                </p>
                {isLoading ? (
                  <ListSkeleton compact />
                ) : !aiUsage?.byClass.length ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : (
                  <ul className="space-y-2">
                    {aiUsage.byClass.map((c) => (
                      <li
                        key={c.classId}
                        className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                      >
                        <span>{c.className}</span>
                        <span className="font-data text-muted-foreground">{c.callCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <div>
                <p className="mb-2 text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  Fanlar bo&apos;yicha
                </p>
                {isLoading ? (
                  <ListSkeleton compact />
                ) : !aiUsage?.bySubject.length ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : (
                  <ul className="space-y-2">
                    {aiUsage.bySubject.map((s) => (
                      <li
                        key={s.subjectId}
                        className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                      >
                        <span>{s.subjectName}</span>
                        <span className="font-data text-muted-foreground">{s.callCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <div>
                <p className="mb-2 text-xs font-medium text-muted-foreground uppercase tracking-wide">
                  O&apos;qituvchilar bo&apos;yicha
                </p>
                {isLoading ? (
                  <ListSkeleton compact />
                ) : !aiUsage?.byTeacher.length ? (
                  <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
                ) : (
                  <ul className="space-y-2">
                    {aiUsage.byTeacher.map((t) => (
                      <li
                        key={t.teacherId}
                        className="flex items-center justify-between rounded-md border border-border px-3 py-2 text-sm"
                      >
                        <span>
                          {t.firstName} {t.lastName}
                        </span>
                        <span className="font-data text-muted-foreground">{t.callCount}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  );
}

function ListSkeleton({ compact }: { compact?: boolean }) {
  return (
    <div className="space-y-2">
      <Skeleton className={compact ? "h-8 w-full" : "h-10 w-full"} />
      <Skeleton className={compact ? "h-8 w-full" : "h-10 w-full"} />
      <Skeleton className={compact ? "h-8 w-full" : "h-10 w-full"} />
    </div>
  );
}
