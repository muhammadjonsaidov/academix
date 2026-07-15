"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { Button } from "@/components/ui/button";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function TeacherSubmissionDetailPage() {
  const params = useParams<{ submissionId: string }>();
  const submissionId = params.submissionId;
  const submission = useTeacherStore((state) => state.selectedSubmission);
  const fetchSubmission = useTeacherStore((state) => state.fetchSubmission);
  const gradeSubmission = useTeacherStore((state) => state.gradeSubmission);

  const [fivePointGrade, setFivePointGrade] = useState("5");
  const [teacherComment, setTeacherComment] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchSubmission(submissionId).catch(() => setError("Topshiriqni yuklab bo'lmadi."));
  }, [fetchSubmission, submissionId]);

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

  if (!submission) {
    return (
      <DashboardShell role="TEACHER">
        <TeacherNav />
        {error ? <p className="text-sm text-destructive">{error}</p> : <p>Yuklanmoqda...</p>}
      </DashboardShell>
    );
  }

  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />
      <h2 className="mb-1 text-lg font-semibold">{submission.studentName}</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Topshirilgan: {new Date(submission.submittedAt).toLocaleString()}
        {submission.isLate ? " · Kechikkan" : ""}
      </p>

      {submission.aiFeedback ? (
        <div className="mb-6 space-y-3 rounded-md border border-border p-4">
          <p className="font-medium">AI tahlili — {Math.round(submission.aiFeedback.aiScorePercent)}%</p>
          <p className="text-sm">{submission.aiFeedback.feedback}</p>

          {submission.aiFeedback.criteriaScores.length > 0 ? (
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
                    <td className="py-1">{c.weightPercent}%</td>
                    <td className="py-1">{c.score}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}

          <div className="flex gap-6 text-sm text-muted-foreground">
            <span>Plagiat: {submission.aiFeedback.plagiarismScore} ({submission.aiFeedback.plagiarismType})</span>
            <span>Yozuv mosligi: {submission.aiFeedback.handwritingMatchScore}%</span>
          </div>
        </div>
      ) : (
        <p className="mb-6 text-sm text-muted-foreground">AI tahlili hali mavjud emas.</p>
      )}

      {submission.previousGrade ? (
        <p className="mb-4 text-sm">
          Joriy baho: {submission.previousGrade.score} ({submission.previousGrade.fivePointGrade})
        </p>
      ) : null}

      <form onSubmit={handleGrade} className="flex flex-wrap items-end gap-3">
        <div className="space-y-1">
          <label htmlFor="score" className="text-sm font-medium">
            Ball (0-100)
          </label>
          <input
            key={submissionId}
            ref={scoreRef}
            id="score"
            type="number"
            min={0}
            max={100}
            defaultValue={defaultScore}
            required
            className="w-24 rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="fivePointGrade" className="text-sm font-medium">
            Baho (2-5)
          </label>
          <input
            id="fivePointGrade"
            type="number"
            min={2}
            max={5}
            value={fivePointGrade}
            onChange={(e) => setFivePointGrade(e.target.value)}
            required
            className="w-20 rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="teacherComment" className="text-sm font-medium">
            Izoh
          </label>
          <input
            id="teacherComment"
            value={teacherComment}
            onChange={(e) => setTeacherComment(e.target.value)}
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saqlanmoqda..." : "Baholash"}
        </Button>
      </form>

      {error ? <p className="mt-4 text-sm text-destructive">{error}</p> : null}
    </DashboardShell>
  );
}
