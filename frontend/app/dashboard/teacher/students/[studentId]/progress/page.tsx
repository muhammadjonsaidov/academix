"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { FileCheck2, History, KeyRound, TrendingUp } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { StatusLabel, submissionRailClass } from "@/components/shared/submission-status";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTeacherAnalyticsStore } from "@/stores/useTeacherAnalyticsStore";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { SubmissionStatus } from "@/types/teacher";

export default function TeacherStudentProgressPage() {
  const params = useParams<{ studentId: string }>();
  const progress = useTeacherAnalyticsStore((state) => state.studentProgress);
  const fetchStudentProgress = useTeacherAnalyticsStore((state) => state.fetchStudentProgress);
  const resetStudentPassword = useTeacherStore((state) => state.resetStudentPassword);
  const [error, setError] = useState<string | null>(null);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [tempPassword, setTempPassword] = useState<string | null>(null);
  const [resetError, setResetError] = useState<string | null>(null);

  useEffect(() => {
    fetchStudentProgress(params.studentId).catch(() => setError("Progressni yuklab bo'lmadi."));
  }, [params.studentId, fetchStudentProgress]);

  async function handleResetPassword() {
    const confirmed = window.confirm(
      "Bu o'quvchining parolini reset qilmoqchimisiz? Vaqtinchalik parol shu yerda ko'rsatiladi — uni o'quvchiga shaxsan yetkazing.",
    );
    if (!confirmed) return;
    setResetError(null);
    setIsResettingPassword(true);
    try {
      const password = await resetStudentPassword(params.studentId);
      setTempPassword(password);
    } catch {
      setResetError("Parolni reset qilib bo'lmadi.");
    } finally {
      setIsResettingPassword(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {!progress && !error ? (
        <div className="space-y-4">
          <Skeleton className="h-7 w-56" />
          <Skeleton className="h-40" />
        </div>
      ) : null}

      {progress ? (
        <>
          <div className="mb-4 flex items-center justify-between gap-4">
            <h2 className="font-heading text-lg font-semibold">
              {progress.student.firstName} {progress.student.lastName}
            </h2>
            <Button
              variant="outline"
              size="sm"
              disabled={isResettingPassword}
              onClick={handleResetPassword}
            >
              <KeyRound className="size-3.5" strokeWidth={1.75} />
              {isResettingPassword ? "..." : "Parolni reset qilish"}
            </Button>
          </div>

          {tempPassword ? (
            <Card className="mb-4 border-severity-medium/40 bg-severity-medium-bg">
              <CardContent className="py-3 text-sm">
                Vaqtinchalik parol:{" "}
                <span className="font-data font-semibold">{tempPassword}</span> — buni
                o&apos;quvchiga shaxsan yetkazing, keyin bu xabar ko&apos;rinmay qoladi.
              </CardContent>
            </Card>
          ) : null}
          {resetError ? <p className="mb-4 text-sm text-destructive">{resetError}</p> : null}

          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="size-4 text-muted-foreground" strokeWidth={1.75} />
                Fanlar bo&apos;yicha
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {progress.subjectStats.map((s) => (
                <div key={s.subject} className="rounded-md border border-border px-3 py-2.5 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{s.subject}</span>
                    <span className="font-data text-muted-foreground">
                      {s.averageScore}% ({s.trend})
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Topshirish darajasi: {s.submissionRate}%
                  </p>
                </div>
              ))}
              {progress.subjectStats.length === 0 ? (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              ) : null}
            </CardContent>
          </Card>

          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <History className="size-4 text-muted-foreground" strokeWidth={1.75} />
                XP tarixi
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {progress.xpHistory.slice(0, 10).map((x, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                >
                  <span>{x.reason}</span>
                  <span className="font-data text-muted-foreground">
                    +{x.xp} XP — {new Date(x.date).toLocaleDateString()}
                  </span>
                </div>
              ))}
              {progress.xpHistory.length === 0 ? (
                <p className="text-sm text-muted-foreground">Ma&apos;lumot yo&apos;q.</p>
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileCheck2 className="size-4 text-muted-foreground" strokeWidth={1.75} />
                So&apos;nggi topshirilgan ishlar
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {progress.recentSubmissions.map((s) => (
                <div
                  key={s.submissionId}
                  className={`flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm ${submissionRailClass(s.status as SubmissionStatus)}`}
                >
                  <StatusLabel status={s.status}>{s.status}</StatusLabel>
                  <span className="font-data text-muted-foreground">
                    {new Date(s.submittedAt).toLocaleDateString()}
                    {s.isLate ? " (kech)" : ""}
                  </span>
                </div>
              ))}
              {progress.recentSubmissions.length === 0 ? (
                <EmptyState
                  icon={FileCheck2}
                  title="Ma'lumot yo'q"
                  description="Bu o'quvchining so'nggi topshirilgan ishlari hali mavjud emas."
                />
              ) : null}
            </CardContent>
          </Card>
        </>
      ) : null}
    </DashboardShell>
  );
}
