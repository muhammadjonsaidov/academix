"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { FileCheck2, RefreshCw } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { SubmissionStatusBadge, submissionRailClass } from "@/components/shared/submission-status";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTeacherStore } from "@/stores/useTeacherStore";

export default function TeacherSubmissionsPage() {
  return (
    <Suspense fallback={null}>
      <TeacherSubmissionsList />
    </Suspense>
  );
}

function TeacherSubmissionsList() {
  const searchParams = useSearchParams();
  const assignmentId = searchParams.get("assignmentId") ?? undefined;
  const submissions = useTeacherStore((state) => state.submissions);
  const fetchSubmissions = useTeacherStore((state) => state.fetchSubmissions);
  const [error, setError] = useState<string | null>(null);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    fetchSubmissions(assignmentId ? { assignmentId } : undefined)
      .catch(() => setError("Topshiriqlarni yuklab bo'lmadi."))
      .finally(() => setHasLoaded(true));
  }, [fetchSubmissions, assignmentId]);

  async function handleRefresh() {
    setError(null);
    setIsRefreshing(true);
    try {
      await fetchSubmissions(assignmentId ? { assignmentId } : undefined);
    } catch {
      setError("Topshiriqlarni yuklab bo'lmadi.");
    } finally {
      setIsRefreshing(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-heading text-lg font-semibold">Topshirilgan ishlar</h2>
        <Button variant="outline" onClick={handleRefresh} disabled={isRefreshing}>
          <RefreshCw className={isRefreshing ? "size-4 animate-spin" : "size-4"} strokeWidth={1.75} />
          Yangilash
        </Button>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {!hasLoaded ? (
        <div className="space-y-2">
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
          <Skeleton className="h-12" />
        </div>
      ) : submissions.length === 0 ? (
        <EmptyState
          icon={FileCheck2}
          title="Hozircha topshiriqlar yo'q"
          description="O'quvchilar vazifani topshirganda bu yerda ko'rinadi."
        />
      ) : (
        <Card>
          <CardContent className="px-0">
            <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="px-6 py-2">O&apos;quvchi</th>
                  <th className="py-2">Topshirilgan vaqt</th>
                  <th className="py-2">Holati</th>
                  <th className="py-2">AI ball</th>
                  <th className="px-6 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((s) => (
                  <tr key={s.submissionId} className="border-b border-border last:border-0">
                    <td className={`px-5 py-3 pl-6 font-medium ${submissionRailClass(s.status)}`}>
                      {s.studentName}
                    </td>
                    <td className="py-3 font-data">{new Date(s.submittedAt).toLocaleString()}</td>
                    <td className="py-3">
                      <div className="flex items-center gap-2">
                        <SubmissionStatusBadge status={s.status} />
                        {s.isLate ? <span className="text-xs text-muted-foreground">(kech)</span> : null}
                      </div>
                    </td>
                    <td className="py-3 font-data">
                      {s.aiFeedback ? `${Math.round(s.aiFeedback.aiScorePercent)}%` : "—"}
                    </td>
                    <td className="px-6 py-3 text-right">
                      <Link
                        href={`/dashboard/teacher/submissions/${s.submissionId}`}
                        className="text-sm underline"
                      >
                        Ko&apos;rish
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          </CardContent>
        </Card>
      )}
    </DashboardShell>
  );
}
