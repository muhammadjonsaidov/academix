"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useStudentStore } from "@/stores/useStudentStore";

const STATUS_LABEL: Record<string, string> = {
  SUBMITTED: "Topshirilgan",
  AI_PROCESSING: "AI tahlil qilmoqda",
  AI_DONE: "AI baholadi",
  AI_SKIPPED: "AI o'tkazib yubordi",
  GRADED: "Baholangan",
};

export default function StudentSubmissionsPage() {
  const submissions = useStudentStore((state) => state.submissions);
  const fetchSubmissions = useStudentStore((state) => state.fetchSubmissions);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSubmissions().catch(() => setError("Topshiriqlarni yuklab bo'lmadi."));
  }, [fetchSubmissions]);

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">Topshirilgan ishlarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Topshirilgan vaqt</th>
            <th className="py-2">Holati</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {submissions.map((s) => (
            <tr key={s.submissionId} className="border-b border-border">
              <td className="py-2">{new Date(s.submittedAt).toLocaleString()}</td>
              <td className="py-2">
                {STATUS_LABEL[s.status] ?? s.status}
                {s.isLate ? " (kech)" : ""}
              </td>
              <td className="py-2 text-right">
                <Link
                  href={`/dashboard/student/submissions/${s.submissionId}`}
                  className="text-sm underline"
                >
                  Ko&apos;rish
                </Link>
              </td>
            </tr>
          ))}
          {submissions.length === 0 ? (
            <tr>
              <td colSpan={3} className="py-4 text-center text-muted-foreground">
                Hozircha topshiriqlar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
