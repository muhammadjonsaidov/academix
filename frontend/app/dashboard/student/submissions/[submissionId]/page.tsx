"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useStudentStore } from "@/stores/useStudentStore";

export default function StudentSubmissionDetailPage() {
  const params = useParams<{ submissionId: string }>();
  const submissionId = params.submissionId;
  const submission = useStudentStore((state) => state.selectedSubmission);
  const fetchSubmission = useStudentStore((state) => state.fetchSubmission);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSubmission(submissionId).catch(() => setError("Topshiriqni yuklab bo'lmadi."));
  }, [fetchSubmission, submissionId]);

  if (!submission) {
    return (
      <DashboardShell role="STUDENT">
        <StudentNav />
        {error ? <p className="text-sm text-destructive">{error}</p> : <p>Yuklanmoqda...</p>}
      </DashboardShell>
    );
  }

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-1 text-lg font-semibold">Topshiriq natijasi</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Topshirilgan: {new Date(submission.submittedAt).toLocaleString()}
        {submission.isLate ? " · Kechikkan" : ""}
      </p>

      {submission.aiFeedback ? (
        <div className="mb-6 space-y-3 rounded-md border border-border p-4">
          <p className="font-medium">AI fikri</p>
          <p className="text-sm">{submission.aiFeedback.feedback}</p>

          {submission.aiFeedback.criteriaScores.length > 0 ? (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr>
                  <th className="py-1">Mezon</th>
                  <th className="py-1">Ball</th>
                </tr>
              </thead>
              <tbody>
                {submission.aiFeedback.criteriaScores.map((c) => (
                  <tr key={c.name}>
                    <td className="py-1">{c.name}</td>
                    <td className="py-1">{c.score}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}

          {submission.aiFeedback.stepAnalyses.length > 0 ? (
            <ul className="space-y-1 text-sm">
              {submission.aiFeedback.stepAnalyses.map((step) => (
                <li key={step.stepNumber}>
                  {step.stepNumber}. {step.stepContent} — {step.isCorrect ? "✓" : "✗"}
                  {step.errorDescription ? ` (${step.errorDescription})` : ""}
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : (
        <p className="mb-6 text-sm text-muted-foreground">AI tahlili hali mavjud emas.</p>
      )}

      {submission.grade ? (
        <div className="rounded-md border border-border p-4">
          <p className="font-medium">
            Baho: {submission.grade.score} ({submission.grade.fivePointGrade})
          </p>
          {submission.grade.teacherComment ? (
            <p className="mt-2 text-sm text-muted-foreground">{submission.grade.teacherComment}</p>
          ) : null}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Hali baholanmagan.</p>
      )}
    </DashboardShell>
  );
}
