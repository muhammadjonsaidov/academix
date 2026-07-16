"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useExamStore } from "@/stores/useExamStore";

export default function StudentExamDetailPage() {
  const params = useParams<{ examId: string }>();
  const examId = params.examId;
  const exam = useExamStore((state) => state.selectedStudentExam);
  const fetchStudentExamDetail = useExamStore((state) => state.fetchStudentExamDetail);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStudentExamDetail(examId).catch(() => setError("Nazorat ishini yuklab bo'lmadi."));
  }, [fetchStudentExamDetail, examId]);

  if (!exam) {
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
      <h2 className="mb-1 text-lg font-semibold">{exam.title}</h2>
      <p className="mb-4 text-sm text-muted-foreground">{exam.examDate}</p>

      {exam.aiFeedback ? (
        <div className="mb-6 space-y-3 rounded-md border border-border p-4">
          <p className="font-medium">AI fikri</p>
          <p className="text-sm">{exam.aiFeedback.feedback}</p>

          {exam.aiFeedback.criteriaScores.length > 0 ? (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr>
                  <th className="py-1">Mezon</th>
                  <th className="py-1">Ball</th>
                </tr>
              </thead>
              <tbody>
                {exam.aiFeedback.criteriaScores.map((c) => (
                  <tr key={c.name}>
                    <td className="py-1">{c.name}</td>
                    <td className="py-1">{c.score}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}

          {exam.aiFeedback.stepAnalyses.length > 0 ? (
            <ul className="space-y-1 text-sm">
              {exam.aiFeedback.stepAnalyses.map((step) => (
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

      {exam.grade ? (
        <div className="rounded-md border border-border p-4">
          <p className="font-medium">
            Baho: {exam.grade.score} ({exam.grade.fivePointGrade})
          </p>
          {exam.grade.teacherComment ? (
            <p className="mt-2 text-sm text-muted-foreground">{exam.grade.teacherComment}</p>
          ) : null}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Hali baholanmagan.</p>
      )}
    </DashboardShell>
  );
}
