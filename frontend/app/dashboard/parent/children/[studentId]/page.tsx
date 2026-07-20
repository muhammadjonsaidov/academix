"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  CheckCircle2,
  ClipboardList,
  Download,
  GraduationCap,
  Minus,
  ShieldAlert,
  TrendingDown,
  TrendingUp,
  type LucideIcon,
} from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useConsentStore } from "@/stores/useConsentStore";
import { useParentStore } from "@/stores/useParentStore";
import type { ApiErrorResponse } from "@/types/auth";

const TREND_CONFIG: Record<
  string,
  { label: string; icon: LucideIcon; variant: "success" | "destructive" | "outline" }
> = {
  UP: { label: "O'sish", icon: TrendingUp, variant: "success" },
  DOWN: { label: "Pasayish", icon: TrendingDown, variant: "destructive" },
  STABLE: { label: "Barqaror", icon: Minus, variant: "outline" },
};

export default function ParentChildDetailPage() {
  const params = useParams<{ studentId: string }>();
  const studentId = params.studentId;

  const overview = useParentStore((state) => state.selectedOverview);
  const progress = useParentStore((state) => state.selectedProgress);
  const grades = useParentStore((state) => state.selectedGrades);
  const homework = useParentStore((state) => state.selectedHomework);
  const fetchOverview = useParentStore((state) => state.fetchOverview);
  const fetchProgress = useParentStore((state) => state.fetchProgress);
  const fetchGrades = useParentStore((state) => state.fetchGrades);
  const fetchHomework = useParentStore((state) => state.fetchHomework);
  const downloadSemesterReport = useParentStore((state) => state.downloadSemesterReport);
  const requestDataDeletion = useConsentStore((state) => state.requestDataDeletion);

  const [error, setError] = useState<string | null>(null);
  const [deletionMessage, setDeletionMessage] = useState<string | null>(null);
  const [isRequestingDeletion, setIsRequestingDeletion] = useState(false);
  const [isProgressLoading, setIsProgressLoading] = useState(true);
  const [isGradesLoading, setIsGradesLoading] = useState(true);
  const [isHomeworkLoading, setIsHomeworkLoading] = useState(true);

  useEffect(() => {
    fetchOverview(studentId).catch(() => setError("Ma'lumotlarni yuklab bo'lmadi."));
    fetchProgress(studentId)
      .catch(() => {})
      .finally(() => setIsProgressLoading(false));
    fetchGrades(studentId)
      .catch(() => {})
      .finally(() => setIsGradesLoading(false));
    fetchHomework(studentId)
      .catch(() => {})
      .finally(() => setIsHomeworkLoading(false));
  }, [studentId, fetchOverview, fetchProgress, fetchGrades, fetchHomework]);

  async function handleDataDeletionRequest() {
    setError(null);
    setDeletionMessage(null);
    setIsRequestingDeletion(true);
    try {
      await requestDataDeletion(studentId);
      setDeletionMessage("So'rov yuborildi. Administrator tasdiqlashini kuting.");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "So'rov yuborib bo'lmadi.");
    } finally {
      setIsRequestingDeletion(false);
    }
  }

  if (!overview) {
    return (
      <DashboardShell role="PARENT">
        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : (
          <div className="space-y-4">
            <Card>
              <CardContent className="space-y-3 pt-6">
                <Skeleton className="h-5 w-1/3" />
                <Skeleton className="h-4 w-1/4" />
              </CardContent>
            </Card>
            <Card>
              <CardContent className="space-y-3 pt-6">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-2/3" />
              </CardContent>
            </Card>
          </div>
        )}
      </DashboardShell>
    );
  }

  const pendingHomework = homework.filter((h) => h.submissionStatus === "PENDING");

  return (
    <DashboardShell role="PARENT">
      <Card className="mb-6">
        <CardContent className="flex flex-col gap-4 pt-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold">
              {overview.summary.name} — {overview.summary.className}
            </h2>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <Badge variant={overview.summary.todayActivity ? "success" : "outline"}>
                {overview.summary.todayActivity ? "Bugun faol bo'lgan" : "Bugun faollik yo'q"}
              </Badge>
              {overview.summary.pendingHomeworkCount > 0 ? (
                <Badge variant="outline">
                  {overview.summary.pendingHomeworkCount} ta bajarilmagan vazifa
                </Badge>
              ) : (
                <Badge variant="success">Barcha vazifalar bajarilgan</Badge>
              )}
            </div>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => downloadSemesterReport(studentId)}
          >
            <Download className="size-3.5" strokeWidth={1.75} />
            Semestr hisobotini yuklab olish
          </Button>
        </CardContent>
      </Card>

      {error ? (
        <p role="alert" className="mb-4 text-sm text-destructive">
          {error}
        </p>
      ) : null}

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Fanlar bo&apos;yicha rivojlanish</CardTitle>
          <CardDescription>
            Sinf o&apos;rtachasi yoki boshqa o&apos;quvchilar bilan solishtirish
            ko&apos;rsatilmaydi.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isProgressLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-2/3" />
            </div>
          ) : progress && progress.subjectProgress.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr>
                    <th className="py-1 font-medium">Fan</th>
                    <th className="py-1 font-medium">Joriy o&apos;rtacha</th>
                    <th className="py-1 font-medium">O&apos;tgan oy</th>
                    <th className="py-1 font-medium">Topshirish darajasi</th>
                    <th className="py-1 font-medium">Tendensiya</th>
                  </tr>
                </thead>
                <tbody>
                  {progress.subjectProgress.map((sp) => {
                    const trend = TREND_CONFIG[sp.trend] ?? TREND_CONFIG.STABLE;
                    const TrendIcon = trend.icon;
                    return (
                      <tr key={sp.subject} className="border-t border-border">
                        <td className="py-2">{sp.subject}</td>
                        <td className="py-2 font-data">{sp.currentAvg.toFixed(1)}</td>
                        <td className="py-2 font-data">{sp.previousMonthAvg.toFixed(1)}</td>
                        <td className="py-2 font-data">{Math.round(sp.submissionRate * 100)}%</td>
                        <td className="py-2">
                          <Badge variant={trend.variant}>
                            <TrendIcon className="size-3" strokeWidth={1.75} />
                            {trend.label}
                          </Badge>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border py-8 text-center">
              <TrendingUp className="size-6 text-muted-foreground" strokeWidth={1.75} />
              <p className="text-sm text-muted-foreground">Hozircha rivojlanish ma&apos;lumoti yo&apos;q.</p>
            </div>
          )}

          {progress && progress.badges.length > 0 ? (
            <div className="mt-4">
              <p className="mb-2 text-sm font-medium">Yutuqlar</p>
              <div className="flex flex-wrap gap-2">
                {progress.badges.map((b) => (
                  <span
                    key={b.name}
                    className="inline-flex items-center gap-1.5 rounded-md border border-border px-2.5 py-1 text-sm"
                  >
                    <span>{b.icon}</span>
                    <span>{b.name}</span>
                  </span>
                ))}
              </div>
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Baholar</CardTitle>
        </CardHeader>
        <CardContent>
          {isGradesLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : grades && grades.recent.length > 0 ? (
            <ul className="space-y-2">
              {grades.recent.map((g) => (
                <li
                  key={g.submissionId}
                  className="rail-verified flex items-center justify-between rounded-md bg-muted/40 px-3 py-2 text-sm"
                >
                  <span>{g.subject}</span>
                  <span className="font-data font-medium">
                    {g.score} ball ({g.fivePointGrade})
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <div className="flex flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border py-8 text-center">
              <GraduationCap className="size-6 text-muted-foreground" strokeWidth={1.75} />
              <p className="text-sm text-muted-foreground">Hozircha baholar yo&apos;q.</p>
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Bajarilmagan vazifalar</CardTitle>
        </CardHeader>
        <CardContent>
          {isHomeworkLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : pendingHomework.length > 0 ? (
            <ul className="space-y-2">
              {pendingHomework.map((h) => (
                <li
                  key={h.assignmentId}
                  className="flex flex-col gap-1 rounded-md border border-border px-3 py-2 text-sm sm:flex-row sm:items-center sm:justify-between"
                >
                  <span>
                    {h.subjectName} — {h.title}
                  </span>
                  <span className="flex items-center gap-2">
                    {h.isLate ? <Badge variant="destructive">Kechikkan</Badge> : null}
                    <span className="font-data text-muted-foreground">
                      {new Date(h.deadlineAt).toLocaleDateString()}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <div className="flex flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border py-8 text-center">
              <ClipboardList className="size-6 text-muted-foreground" strokeWidth={1.75} />
              <p className="text-sm text-muted-foreground">Bajarilmagan vazifalar yo&apos;q.</p>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ShieldAlert className="size-4 text-muted-foreground" strokeWidth={1.75} />
            Ma&apos;lumotlarni o&apos;chirish so&apos;rovi
          </CardTitle>
          <CardDescription>
            Faqat o&apos;quvchi maktabni tark etgandan so&apos;ng yuborish mumkin.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={isRequestingDeletion}
            onClick={handleDataDeletionRequest}
          >
            {isRequestingDeletion ? "..." : "So'rov yuborish"}
          </Button>
          {deletionMessage ? (
            <p className="flex items-center gap-1.5 text-sm text-success">
              <CheckCircle2 className="size-3.5" strokeWidth={1.75} />
              {deletionMessage}
            </p>
          ) : null}
        </CardContent>
      </Card>
    </DashboardShell>
  );
}
