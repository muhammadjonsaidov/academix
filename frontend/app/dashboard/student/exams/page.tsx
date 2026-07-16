"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useExamStore } from "@/stores/useExamStore";

export default function StudentExamsPage() {
  const studentExams = useExamStore((state) => state.studentExams);
  const fetchStudentExams = useExamStore((state) => state.fetchStudentExams);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchStudentExams().catch(() => setError("Nazorat ishlarini yuklab bo'lmadi."));
  }, [fetchStudentExams]);

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">Nazorat ishlarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Sarlavha</th>
            <th className="py-2">Fan</th>
            <th className="py-2">Sana</th>
            <th className="py-2">Baho</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {studentExams.map((exam) => (
            <tr key={exam.examId} className="border-b border-border">
              <td className="py-2 font-medium">{exam.title}</td>
              <td className="py-2">{exam.subject}</td>
              <td className="py-2">{exam.examDate}</td>
              <td className="py-2">
                {exam.myGrade ? `${exam.myGrade.score} (${exam.myGrade.fivePointGrade})` : "—"}
              </td>
              <td className="py-2 text-right">
                <Link href={`/dashboard/student/exams/${exam.examId}`} className="text-sm underline">
                  Ko&apos;rish
                </Link>
              </td>
            </tr>
          ))}
          {studentExams.length === 0 ? (
            <tr>
              <td colSpan={5} className="py-4 text-center text-muted-foreground">
                Hozircha nazorat ishlari yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
