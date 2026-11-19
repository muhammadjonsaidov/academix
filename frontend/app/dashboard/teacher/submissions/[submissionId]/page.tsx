"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, RefreshCw, UserCheck } from "lucide-react";
import { DashboardShell } from "@/components/shell/DashboardShell";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { GradeMark, PencilScore } from "@/components/shared/GradeMark";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { InkMark } from "@/components/ui/ink-mark";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { subscribeToAiStatus } from "@/hooks/useRealtime";
import { formatDateTime } from "@/lib/format";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { GradingMode, PlagiarismType } from "@/types/teacher";

/** What the AI's analysis rows are, per checking mode (docs/design.md). */
const MODE_COPY: Record<GradingMode, { title: string; hint: string }> = {
  PROBLEM_SOLVING: { title: "Qadamlar", hint: "Yechimning har bir qadami tekshirildi." },
  LANGUAGE: { title: "Xatolar va to'g'risi", hint: "Imlo, grammatika va tinish belgilari." },
  WRITTEN_RESPONSE: { title: "Asosiy fikrlar", hint: "Faktlar, to'liqlik va dalillar." },
};

/** Same scale as the backend's FivePointScale: 86+ → 5, 71+ → 4, 56+ → 3, else 2. */
function gradeFor(score: number): number {
  const rounded = Math.round(score);
  if (rounded >= 86) return 5;
  if (rounded >= 71) return 4;
  if (rounded >= 56) return 3;
  return 2;
}

function originalityText(type: PlagiarismType, score: number): { text: string; flagged: boolean } {
  const percent = Math.round(score * (score <= 1 ? 100 : 1));
  if (type === "AI_GENERATED") return { text: `AI yozgan bo'lishi mumkin (${percent}%)`, flagged: true };
  if (type === "CLEAN") return { text: "Mustaqil yozilgan", flagged: false };
  return { text: `Ko'chirilgan bo'lishi mumkin (${percent}%)`, flagged: true };
}

export default function TeacherSubmissionDetailPage() {
  const params = useParams<{ submissionId: string }>();
  const submissionId = params.submissionId;
  const router = useRouter();
  const submission = useTeacherStore((state) => state.selectedSubmission);
  const fetchSubmission = useTeacherStore((state) => state.fetchSubmission);
  const fetchSubmissionImage = useTeacherStore((state) => state.fetchSubmissionImage);
  const findNextToReview = useTeacherStore((state) => state.findNextToReview);

  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    fetchSubmission(submissionId).catch(() => setError("Ishni yuklab bo'lmadi. Sahifani yangilang."));
  }, [fetchSubmission, submissionId]);

  useEffect(() => {
    let url: string | null = null;
    let cancelled = false;
    fetchSubmissionImage(submissionId)
      .then((result) => {
        url = result;
        if (!cancelled) setImageUrl(result);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [fetchSubmissionImage, submissionId]);

  // The page refreshes itself when the AI finishes this submission.
  useEffect(() => {
    const unsubscribe = subscribeToAiStatus((payload) => {
      if (payload.submissionId === submissionId && payload.status !== "AI_PROCESSING") {
        fetchSubmission(submissionId).catch(() => {});
      }
    });
    return unsubscribe;
  }, [submissionId, fetchSubmission]);

  async function handleRefresh() {
    setError(null);
    setIsRefreshing(true);
    try {
      await fetchSubmission(submissionId);
    } catch {
      setError("Ishni yuklab bo'lmadi. Sahifani yangilang.");
    } finally {
      setIsRefreshing(false);
    }
  }

  async function goToNext() {
    if (!submission) return;
    const next = await findNextToReview(submission.assignmentId, submission.submissionId).catch(
      () => null,
    );
    router.push(
      next
        ? `/dashboard/teacher/submissions/${next}`
        : `/dashboard/teacher/submissions?assignmentId=${submission.assignmentId}&status=AI_DONE`,
    );
  }

  if (!submission || submission.submissionId !== submissionId) {
    return (
      <DashboardShell role="TEACHER">
        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : (
          <div className="space-y-3">
            <Skeleton className="h-8 w-64" />
            <div className="grid gap-6 lg:grid-cols-2">
              <Skeleton className="h-96" />
              <Skeleton className="h-96" />
            </div>
          </div>
        )}
      </DashboardShell>
    );
  }

  const feedback = submission.aiFeedback;
  const isProcessing = submission.status === "SUBMITTED" || submission.status === "AI_PROCESSING";
  const isSkipped = submission.status === "AI_SKIPPED";
  const mode = MODE_COPY[submission.gradingMode ?? "PROBLEM_SOLVING"];
  const originality = feedback ? originalityText(feedback.plagiarismType, feedback.plagiarismScore) : null;

  return (
    <DashboardShell role="TEACHER">
      <div className="space-y-6">
        <div className="space-y-2">
          <Link
            href={`/dashboard/teacher/submissions?assignmentId=${submission.assignmentId}`}
            className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" strokeWidth={1.75} />
            {submission.assignmentTitle ?? "Topshirilgan ishlar"}
          </Link>
          <h1 className="text-2xl font-semibold tracking-tight">{submission.studentName}</h1>
          <p className="text-sm text-muted-foreground">
            Topshirildi {formatDateTime(submission.submittedAt)}
            {submission.isLate ? ", muddatdan kech" : ""}
          </p>
        </div>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          {/* Left: the student's own work, exactly as handed in. */}
          <section aria-labelledby="work-heading" className="space-y-3 lg:sticky lg:top-20">
            <h2 id="work-heading" className="text-base font-semibold">
              O&apos;quvchi ishi
            </h2>
            {imageUrl ? (
              <a href={imageUrl} target="_blank" rel="noreferrer" className="block">
                {/* eslint-disable-next-line @next/next/no-img-element -- object URL from an authenticated fetch */}
                <img
                  src={imageUrl}
                  alt={`${submission.studentName} ishining surati`}
                  className="max-h-[70vh] w-full rounded-[10px] border border-border bg-card object-contain"
                />
              </a>
            ) : null}
            {feedback?.extractedText ? (
              imageUrl ? (
                <details className="text-sm">
                  <summary className="cursor-pointer text-muted-foreground select-none">
                    AI o&apos;qigan matn
                  </summary>
                  <p className="mt-2 rounded-md border border-border bg-card p-3 whitespace-pre-wrap">
                    {feedback.extractedText}
                  </p>
                </details>
              ) : (
                <p className="notebook-sheet rounded-[10px] border border-border py-3 pr-4 pl-14 text-[0.9375rem] leading-8 whitespace-pre-wrap">
                  {feedback.extractedText}
                </p>
              )
            ) : !imageUrl ? (
              <p className="text-sm text-muted-foreground">Ish matni hali o&apos;qilmagan.</p>
            ) : null}
          </section>

          {/* Right: the AI's pencil notes, then the teacher's red pen. */}
          <div className="space-y-6">
            {isProcessing ? (
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-[10px] border border-dashed border-pencil/50 bg-card p-4">
                <div className="flex items-center gap-2">
                  <Badge variant="status-processing" className="gap-1.5">
                    <Spinner />
                    AI tekshirmoqda
                  </Badge>
                  <span className="text-sm text-muted-foreground">Natija tayyor bo&apos;lishi bilan shu yerda chiqadi.</span>
                </div>
                <Button variant="outline" size="sm" onClick={handleRefresh} disabled={isRefreshing}>
                  <RefreshCw className={isRefreshing ? "size-4 animate-spin" : "size-4"} strokeWidth={1.75} />
                  Yangilash
                </Button>
              </div>
            ) : null}

            {isSkipped ? (
              <div className="flex items-start gap-3 rounded-[10px] border border-border bg-card p-4">
                <UserCheck className="mt-0.5 size-5 shrink-0 text-pencil" strokeWidth={1.75} />
                <div>
                  <p className="font-medium">Bu ishni o&apos;zingiz baholaysiz</p>
                  <p className="text-sm text-muted-foreground">
                    Maktabning shu oylik AI limiti tugagan, shuning uchun AI bu ishni tekshirmadi. Ish qabul qilingan.
                  </p>
                </div>
              </div>
            ) : null}

            {feedback ? (
              <section aria-labelledby="ai-heading" className="space-y-4 rounded-[10px] border border-border bg-card p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h2 id="ai-heading" className="text-base font-semibold">
                      AI tahlili
                    </h2>
                    <p className="text-sm text-muted-foreground">Taklif — yakuniy bahoni siz qo&apos;yasiz.</p>
                  </div>
                  <PencilScore
                    percent={feedback.aiScorePercent}
                    grade={submission.suggestedGrade}
                    className="text-xl"
                  />
                </div>

                {originality ? (
                  <p className="text-sm">
                    {originality.flagged ? (
                      <span className="marker font-medium">{originality.text}</span>
                    ) : (
                      <span className="text-muted-foreground">{originality.text}</span>
                    )}
                  </p>
                ) : null}

                {feedback.feedback ? <p className="text-[0.9375rem]">{feedback.feedback}</p> : null}

                {feedback.stepAnalyses.length > 0 ? (
                  <div className="space-y-2">
                    <div className="flex items-baseline justify-between gap-2">
                      <h3 className="text-sm font-semibold">{mode.title}</h3>
                      <span className="text-xs text-muted-foreground">{mode.hint}</span>
                    </div>
                    <ol className="divide-y divide-border rounded-md border border-border">
                      {feedback.stepAnalyses.map((step) => (
                        <li key={step.stepNumber} className="flex items-start gap-2.5 px-3 py-2 text-sm">
                          {step.isCorrect ? (
                            <InkMark variant="check" className="mt-0.5 size-4 shrink-0" />
                          ) : (
                            <InkMark variant="cross" label="Xato" className="mt-0.5 size-4 shrink-0" />
                          )}
                          <div className="min-w-0 space-y-0.5">
                            <p className={step.isCorrect ? "" : "text-pen-red line-through decoration-pen-red/60"}>
                              {step.stepContent}
                            </p>
                            {step.suggestion && !step.isCorrect ? (
                              <p className="font-medium text-success">{step.suggestion}</p>
                            ) : null}
                            {!step.isCorrect && step.errorDescription ? (
                              <p className="text-muted-foreground">{step.errorDescription}</p>
                            ) : null}
                          </div>
                        </li>
                      ))}
                    </ol>
                  </div>
                ) : null}

                {feedback.criteriaScores.length > 0 ? (
                  <table className="w-full text-left text-sm">
                    <caption className="sr-only">Mezonlar bo&apos;yicha ball</caption>
                    <thead className="text-muted-foreground">
                      <tr>
                        <th scope="col" className="py-1 font-medium">Mezon</th>
                        <th scope="col" className="py-1 text-right font-medium">Vazni</th>
                        <th scope="col" className="py-1 text-right font-medium">Ball</th>
                      </tr>
                    </thead>
                    <tbody>
                      {feedback.criteriaScores.map((c) => (
                        <tr key={c.name} className="border-t border-border">
                          <td className="py-1.5">{c.name}</td>
                          <td className="py-1.5 text-right font-data text-muted-foreground">{c.weightPercent}%</td>
                          <td className="py-1.5 text-right font-data">{c.score}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : null}
              </section>
            ) : null}

            <GradeForm key={submission.submissionId} onNext={goToNext} />
          </div>
        </div>
      </div>
    </DashboardShell>
  );
}

function GradeForm({ onNext }: { onNext: () => void }) {
  const submission = useTeacherStore((state) => state.selectedSubmission)!;
  const gradeSubmission = useTeacherStore((state) => state.gradeSubmission);
  const previous = submission.previousGrade;
  const aiScore = submission.aiFeedback ? Math.round(submission.aiFeedback.aiScorePercent) : null;

  const [score, setScore] = useState(String(previous?.score ?? aiScore ?? ""));
  const [grade, setGrade] = useState(String(previous?.fivePointGrade ?? submission.suggestedGrade ?? ""));
  const [gradeTouched, setGradeTouched] = useState(!!previous);
  const [comment, setComment] = useState(previous?.teacherComment ?? "");
  const [isExcellent, setIsExcellent] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [savedGrade, setSavedGrade] = useState<number | null>(null);

  function handleScoreChange(value: string) {
    setScore(value);
    // The grade follows the score until the teacher picks one themselves.
    if (!gradeTouched && value !== "" && !Number.isNaN(Number(value))) {
      setGrade(String(gradeFor(Number(value))));
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await gradeSubmission(submission.submissionId, {
        score: Number(score),
        fivePointGrade: Number(grade),
        teacherComment: comment,
        isExcellent,
      });
      setSavedGrade(Number(grade));
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Bahoni saqlab bo'lmadi. Qayta urinib ko'ring.");
    } finally {
      setSaving(false);
    }
  }

  const confirmed = savedGrade ?? (submission.status === "GRADED" ? previous?.fivePointGrade ?? null : null);

  return (
    <section aria-labelledby="grade-heading" className="space-y-4 rounded-[10px] border border-border bg-card p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 id="grade-heading" className="text-base font-semibold">
            {confirmed ? "Baho qo'yilgan" : "Baho"}
          </h2>
          <p className="text-sm text-muted-foreground">
            {confirmed
              ? "O'quvchi va ota-onasi bahoni ko'radi. Kerak bo'lsa o'zgartirishingiz mumkin."
              : "AI taklifini tasdiqlang yoki o'zgartiring."}
          </p>
        </div>
        {confirmed ? <GradeMark grade={confirmed} draw={savedGrade !== null} /> : null}
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField label="Ball (0–100)" htmlFor="score">
            <input
              id="score"
              type="number"
              inputMode="numeric"
              min={0}
              max={100}
              value={score}
              onChange={(e) => handleScoreChange(e.target.value)}
              required
              className={`${fieldClass} w-full font-data`}
            />
          </FormField>
          <FormField label="Baho" htmlFor="fivePointGrade">
            <select
              id="fivePointGrade"
              value={grade}
              onChange={(e) => {
                setGrade(e.target.value);
                setGradeTouched(true);
              }}
              required
              className={`${fieldClass} w-full`}
            >
              <option value="" disabled>
                Tanlang
              </option>
              <option value="5">5 — a&apos;lo</option>
              <option value="4">4 — yaxshi</option>
              <option value="3">3 — qoniqarli</option>
              <option value="2">2 — qoniqarsiz</option>
            </select>
          </FormField>
        </div>
        <FormField label="O'quvchiga izoh" htmlFor="teacherComment">
          <textarea
            id="teacherComment"
            rows={2}
            maxLength={2000}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Masalan: 3-misolda ishorani unutding, qolgani to'g'ri."
            className={`${fieldClass} w-full resize-y py-2`}
          />
        </FormField>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={isExcellent}
            onChange={(e) => setIsExcellent(e.target.checked)}
            className="size-4 accent-[var(--ink)]"
          />
          Namunali ish deb belgilash (+20 XP)
        </label>
        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : null}
        <div className="flex flex-wrap gap-2">
          <Button type="submit" disabled={saving}>
            {saving ? "Saqlanmoqda…" : confirmed ? "Bahoni yangilash" : "Bahoni tasdiqlash"}
          </Button>
          {confirmed ? (
            <Button type="button" variant="outline" onClick={onNext}>
              Keyingi ish
            </Button>
          ) : null}
        </div>
      </form>
    </section>
  );
}
