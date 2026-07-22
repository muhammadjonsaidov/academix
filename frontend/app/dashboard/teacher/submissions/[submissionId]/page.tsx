"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { RefreshCw, UserCheck } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass, FormField, SelectField } from "@/components/shared/FormField";
import { SubmissionStatusBadge, submissionRailClass } from "@/components/shared/submission-status";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { InkMark } from "@/components/ui/ink-mark";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { ResetReason } from "@/types/teacher";

const RESET_REASONS: { value: ResetReason; label: string }[] = [
  { value: "ILLNESS", label: "Kasallik" },
  { value: "INJURY", label: "Jarohat" },
  { value: "TRANSFER_STUDENT", label: "Boshqa maktabdan kelgan" },
  { value: "OTHER", label: "Boshqa" },
];

export default function TeacherSubmissionDetailPage() {
  const params = useParams<{ submissionId: string }>();
  const submissionId = params.submissionId;
  const submission = useTeacherStore((state) => state.selectedSubmission);
  const fetchSubmission = useTeacherStore((state) => state.fetchSubmission);
  const gradeSubmission = useTeacherStore((state) => state.gradeSubmission);
  const resetHandwritingProfile = useTeacherStore((state) => state.resetHandwritingProfile);

  const [fivePointGrade, setFivePointGrade] = useState("5");
  const [teacherComment, setTeacherComment] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const [resetReason, setResetReason] = useState<ResetReason>("ILLNESS");
  const [resetNotes, setResetNotes] = useState("");
  const [resetError, setResetError] = useState<string | null>(null);
  const [resetResult, setResetResult] = useState<string | null>(null);
  const [isResetting, setIsResetting] = useState(false);

  useEffect(() => {
    fetchSubmission(submissionId).catch(() => setError("Topshiriqni yuklab bo'lmadi."));
  }, [fetchSubmission, submissionId]);

  async function handleRefresh() {
    setError(null);
    setIsRefreshing(true);
    try {
      await fetchSubmission(submissionId);
    } catch {
      setError("Topshiriqni yuklab bo'lmadi.");
    } finally {
      setIsRefreshing(false);
    }
  }

  const defaultScore = submission?.aiFeedback
    ? String(Math.round(submission.aiFeedback.aiScorePercent))
    : "";

  const scoreRef = useRef<HTMLInputElement>(null);

  async function handleGrade(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await gradeSubmission(submissionId, {
        score: Number(scoreRef.current?.value ?? 0),
        fivePointGrade: Number(fivePointGrade),
        teacherComment,
      });
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Baholab bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleReset(event: FormEvent) {
    event.preventDefault();
    if (!submission) return;
    setResetError(null);
    setResetResult(null);
    setIsResetting(true);
    try {
      const result = await resetHandwritingProfile(submission.studentId, {
        reason: resetReason,
        notes: resetNotes || undefined,
      });
      setResetResult(
        `Profil qayta tiklandi: ${result.newProfileVersion} (bu chorakda ${result.resetCountThisQuarter}-marta)`,
      );
      setResetNotes("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setResetError(apiError?.message ?? "Yozuv profilini qayta tiklab bo'lmadi.");
    } finally {
      setIsResetting(false);
    }
  }

  if (!submission) {
    return (
      <DashboardShell role="TEACHER">
        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : (
          <div className="space-y-3">
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-40" />
          </div>
        )}
      </DashboardShell>
    );
  }

  const isProcessing = submission.status === "SUBMITTED" || submission.status === "AI_PROCESSING";
  const isSkipped = submission.status === "AI_SKIPPED";

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-1 font-heading text-lg font-semibold">{submission.studentName}</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Topshirilgan: {new Date(submission.submittedAt).toLocaleString()}
        {submission.isLate ? " · Kechikkan" : ""}
      </p>

      {isProcessing ? (
        <Card className="mb-6 rail-processing">
          <CardContent className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Badge variant="status-processing" className="gap-1.5">
                <Spinner />
                AI tahlil qilmoqda
              </Badge>
              <span className="text-sm text-muted-foreground">
                Natija tayyor bo&apos;lganda bu yerda ko&apos;rinadi. Push-xabar yuborilmaydi —
                qo&apos;lda tekshiring.
              </span>
            </div>
            <Button variant="outline" onClick={handleRefresh} disabled={isRefreshing}>
              <RefreshCw className={isRefreshing ? "size-4 animate-spin" : "size-4"} strokeWidth={1.75} />
              Yangilash
            </Button>
          </CardContent>
        </Card>
      ) : null}

      {isSkipped ? (
        <Card className="mb-6 rail-skipped">
          <CardContent className="flex items-center gap-3">
            <UserCheck className="size-5 shrink-0 text-status-skipped" strokeWidth={1.75} />
            <div>
              <p className="font-medium">AI o&apos;tkazib yubordi</p>
              <p className="text-sm text-muted-foreground">
                AI byudjeti tugagani sababli bu ish qo&apos;lda baholanadi.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : null}

      {submission.aiFeedback ? (
        <Card className={`mb-6 ${submissionRailClass(submission.status)}`}>
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <span>AI tahlili</span>
              <SubmissionStatusBadge status={submission.status} />
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="font-data text-2xl font-semibold">
              {Math.round(submission.aiFeedback.aiScorePercent)}%
            </p>
            <p className="text-sm">{submission.aiFeedback.feedback}</p>

            {submission.aiFeedback.extractedText ? (
              <details className="text-sm">
                <summary className="cursor-pointer text-muted-foreground select-none">
                  O&apos;qilgan matn (OCR)
                </summary>
                <p className="mt-2 rounded-md border border-border bg-background/60 p-3 whitespace-pre-wrap">
                  {submission.aiFeedback.extractedText}
                </p>
              </details>
            ) : null}

            {submission.aiFeedback.stepAnalyses.length > 0 ? (
              <div className="space-y-2">
                <p className="text-sm font-medium">Qadam-baqadam tahlil</p>
                {submission.aiFeedback.stepAnalyses.map((step) => (
                  <div
                    key={step.stepNumber}
                    className="flex items-start gap-2.5 rounded-md border border-border px-3 py-2 text-sm"
                  >
                    {step.isCorrect ? (
                      <InkMark variant="check" className="mt-0.5 size-4" />
                    ) : (
                      <InkMark variant="cross" label="Xato" className="mt-0.5 size-4" />
                    )}
                    <div className="min-w-0">
                      <p>{step.stepContent}</p>
                      {!step.isCorrect && step.errorDescription ? (
                        <p className="mt-0.5 text-pen-red">{step.errorDescription}</p>
                      ) : null}
                      {step.suggestion ? (
                        <p className="mt-0.5 text-muted-foreground">{step.suggestion}</p>
                      ) : null}
                    </div>
                  </div>
                ))}
              </div>
            ) : null}

            {submission.aiFeedback.criteriaScores.length > 0 ? (
              <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr>
                    <th className="py-1">Mezon</th>
                    <th className="py-1">Og&apos;irlik</th>
                    <th className="py-1">Ball</th>
                  </tr>
                </thead>
                <tbody>
                  {submission.aiFeedback.criteriaScores.map((c) => (
                    <tr key={c.name}>
                      <td className="py-1">{c.name}</td>
                      <td className="py-1 font-data">{c.weightPercent}%</td>
                      <td className="py-1 font-data">{c.score}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              </div>
            ) : null}

            <div className="flex flex-wrap gap-x-6 gap-y-1 text-sm text-muted-foreground">
              <span>
                Plagiat: <span className="font-data">{submission.aiFeedback.plagiarismScore}</span> (
                {submission.aiFeedback.plagiarismType})
              </span>
              <span>
                Yozuv mosligi:{" "}
                <span className="font-data">{submission.aiFeedback.handwritingMatchScore}%</span>
              </span>
            </div>
          </CardContent>
        </Card>
      ) : !isProcessing && !isSkipped ? (
        <p className="mb-6 text-sm text-muted-foreground">AI tahlili hali mavjud emas.</p>
      ) : null}

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Yozuv profilini qayta tiklash</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-sm text-muted-foreground">
            Faqat shu sinf rahbari o&apos;qituvchisi qayta tiklashi mumkin (chorakda 3 martagacha).
          </p>
          <form onSubmit={handleReset} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <FormField label="Sabab" htmlFor="resetReason">
              <SelectField
                id="resetReason"
                value={resetReason}
                onChange={(e) => setResetReason(e.target.value as ResetReason)}
              >
                {RESET_REASONS.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </SelectField>
            </FormField>
            <FormField label="Izoh (ixtiyoriy)" htmlFor="resetNotes">
              <input
                id="resetNotes"
                value={resetNotes}
                onChange={(e) => setResetNotes(e.target.value)}
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <div className="flex items-end sm:col-span-2 lg:col-span-1">
              <Button type="submit" disabled={isResetting} variant="secondary">
                {isResetting ? "Yuborilmoqda..." : "Qayta tiklash"}
              </Button>
            </div>
          </form>
          {resetResult ? <p className="text-sm text-success">{resetResult}</p> : null}
          {resetError ? <p className="text-sm text-destructive">{resetError}</p> : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Baholash</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {submission.previousGrade ? (
            <p className="text-sm">
              Joriy baho:{" "}
              <span className="font-data">
                {submission.previousGrade.score} ({submission.previousGrade.fivePointGrade})
              </span>
            </p>
          ) : null}

          <form onSubmit={handleGrade} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <FormField label="Ball (0-100)" htmlFor="score">
              <input
                key={submissionId}
                ref={scoreRef}
                id="score"
                type="number"
                min={0}
                max={100}
                defaultValue={defaultScore}
                required
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <FormField label="Baho (2-5)" htmlFor="fivePointGrade">
              <input
                id="fivePointGrade"
                type="number"
                min={2}
                max={5}
                value={fivePointGrade}
                onChange={(e) => setFivePointGrade(e.target.value)}
                required
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <FormField label="Izoh" htmlFor="teacherComment">
              <input
                id="teacherComment"
                value={teacherComment}
                onChange={(e) => setTeacherComment(e.target.value)}
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <div className="flex items-end">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Saqlanmoqda..." : "Baholash"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {error ? <p className="mt-4 text-sm text-destructive">{error}</p> : null}
    </DashboardShell>
  );
}
