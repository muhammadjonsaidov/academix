"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { useTeacherStore } from "@/stores/useTeacherStore";

const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: "Topshirilgan",
  AI_PROCESSING: "AI tahlil qilmoqda",
  AI_DONE: "AI baholadi",
  AI_SKIPPED: "AI o'tkazib yubordi",
  GRADED: "Baholangan",
};

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

  useEffect(() => {
    fetchSubmissions(assignmentId ? { assignmentId } : undefined).catch(() =>
      setError("Topshiriqlarni yuklab bo'lmadi."),
    );
  }, [fetchSubmissions, assignmentId]);

  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />
      <h2 className="mb-4 text-lg font-semibold">Topshirilgan ishlar</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">O&apos;quvchi</th>
            <th className="py-2">Topshirilgan vaqt</th>
            <th className="py-2">Holati</th>
            <th className="py-2">AI ball</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {submissions.map((s) => (
            <tr key={s.submissionId} className="border-b border-border">
              <td className="py-2 font-medium">{s.studentName}</td>
              <td className="py-2">{new Date(s.submittedAt).toLocaleString()}</td>
              <td className="py-2">
                {STATUS_LABEL[s.status] ?? s.status}
                {s.isLate ? " (kech)" : ""}
              </td>
              <td className="py-2">
                {s.aiFeedback ? Math.round(s.aiFeedback.aiScorePercent) : "—"}
              </td>
              <td className="py-2 text-right">
                <Link
                  href={`/dashboard/teacher/submissions/${s.submissionId}`}
                  className="text-sm underline"
                >
                  Ko&apos;rish
                </Link>
              </td>
            </tr>
          ))}
          {submissions.length === 0 ? (
            <tr>
              <td colSpan={5} className="py-4 text-center text-muted-foreground">
                Hozircha topshiriqlar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
