"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { CheckCircle2, RefreshCw, UserCheck, XCircle } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { FULL_STATUS_META } from "@/components/student/submission-status";
import { useExamStore } from "@/stores/useExamStore";
import { cn } from "@/lib/utils";

const REFETCHABLE_STATUSES = new Set(["SUBMITTED", "AI_PROCESSING"]);

export default function StudentExamDetailPage() {
  const params = useParams<{ examId: string }>();
  const examId = params.examId;
  const exam = useExamStore((state) => state.selectedStudentExam);
  const fetchStudentExamDetail = useExamStore((state) => state.fetchStudentExamDetail);
  const [error, setError] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    fetchStudentExamDetail(examId).catch(() => setError("Nazorat ishini yuklab bo'lmadi."));
  }, [fetchStudentExamDetail, examId]);

  async function handleRefresh() {
    setError(null);
    setIsRefreshing(true);
    try {
      await fetchStudentExamDetail(examId);
    } catch {
      setError("Nazorat ishini yuklab bo'lmadi.");
    } finally {
      setIsRefreshing(false);
    }
  }

  if (!exam) {
    return (
      <DashboardShell role="STUDENT">
        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : (
          <div className="space-y-4">
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-32" />
          </div>
        )}
      </DashboardShell>
    );
  }

  const meta = FULL_STATUS_META[exam.status];
  const isProcessing = REFETCHABLE_STATUSES.has(exam.status);
  const isSkipped = exam.status === "AI_SKIPPED";

  return (
    <DashboardShell role="STUDENT">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="font-heading text-lg font-semibold">{exam.title}</h2>
          <p className="text-sm text-muted-foreground">{exam.examDate}</p>
        </div>
        <Badge variant={meta.badgeVariant}>{meta.label}</Badge>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {isProcessing ? (
        <Card className="rail-processing mb-6">
          <CardContent className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Badge variant="status-processing">AI tahlil qilmoqda</Badge>
              <Spinner label="AI tahlil qilmoqda" />
            </div>
            <Button variant="outline" size="sm" onClick={handleRefresh} disabled={isRefreshing}>
              <RefreshCw className={cn("size-3.5", isRefreshing && "animate-spin")} strokeWidth={1.75} />
              Yangilash
            </Button>
          </CardContent>
        </Card>
      ) : null}

      {isSkipped ? (
        <Card className="rail-skipped mb-6">
          <CardContent className="flex items-center gap-2 text-sm">
            <UserCheck className="size-4 shrink-0 text-status-skipped" strokeWidth={1.75} />
            <span>AI byudjeti tugagani sababli bu ish qo&apos;lda baholanadi.</span>
          </CardContent>
        </Card>
      ) : null}

      {exam.aiFeedback ? (
        <Card className={cn("mb-6", meta.rail || undefined)}>
          <CardHeader>
            <CardTitle>AI fikri</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm">{exam.aiFeedback.feedback}</p>

            {exam.aiFeedback.criteriaScores.length > 0 ? (
              <ul className="space-y-1.5">
                {exam.aiFeedback.criteriaScores.map((c) => (
                  <li
                    key={c.name}
                    className="flex items-center justify-between rounded-md bg-muted/40 px-3 py-1.5 text-sm"
                  >
                    <span>
                      {c.name} <span className="text-muted-foreground">({c.weightPercent}%)</span>
                    </span>
                    <span className="font-data font-medium">{c.score}</span>
                  </li>
                ))}
              </ul>
            ) : null}

            {exam.aiFeedback.stepAnalyses.length > 0 ? (
              <ul className="space-y-2">
                {exam.aiFeedback.stepAnalyses.map((step) => (
                  <li key={step.stepNumber} className="flex items-start gap-2 text-sm">
                    {step.isCorrect ? (
                      <CheckCircle2
                        className="mt-0.5 size-4 shrink-0 text-success"
                        strokeWidth={1.75}
                      />
                    ) : (
                      <XCircle
                        className="mt-0.5 size-4 shrink-0 text-destructive"
                        strokeWidth={1.75}
                      />
                    )}
                    <span>
                      <span className="font-medium">{step.stepNumber}.</span> {step.stepContent}
                      {step.errorDescription ? (
                        <span className="block text-muted-foreground">{step.errorDescription}</span>
                      ) : null}
                      {step.suggestion ? (
                        <span className="block text-muted-foreground">💡 {step.suggestion}</span>
                      ) : null}
                    </span>
                  </li>
                ))}
              </ul>
            ) : null}
          </CardContent>
        </Card>
      ) : !isProcessing && !isSkipped ? (
        <p className="mb-6 text-sm text-muted-foreground">AI tahlili hali mavjud emas.</p>
      ) : null}

      {exam.grade ? (
        <Card className="rail-verified">
          <CardHeader>
            <CardTitle>Baho</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="font-data text-2xl font-semibold">
              {exam.grade.score}{" "}
              <span className="text-base font-normal text-muted-foreground">
                ({exam.grade.fivePointGrade})
              </span>
            </p>
            {exam.grade.teacherComment ? (
              <p className="mt-2 text-sm text-muted-foreground">{exam.grade.teacherComment}</p>
            ) : null}
          </CardContent>
        </Card>
      ) : (
        <p className="text-sm text-muted-foreground">Hali baholanmagan.</p>
      )}
    </DashboardShell>
  );
}
