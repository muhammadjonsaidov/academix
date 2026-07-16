"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useStudentStore } from "@/stores/useStudentStore";

export default function StudentXpHistoryPage() {
  const xpHistory = useStudentStore((state) => state.xpHistory);
  const fetchXpHistory = useStudentStore((state) => state.fetchXpHistory);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchXpHistory().catch(() => setError("XP tarixini yuklab bo'lmadi."));
  }, [fetchXpHistory]);

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">XP tarixi</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Sana</th>
            <th className="py-2">XP</th>
            <th className="py-2">Sabab</th>
          </tr>
        </thead>
        <tbody>
          {xpHistory.map((item, i) => (
            <tr key={i} className="border-b border-border">
              <td className="py-2">{new Date(item.date).toLocaleString()}</td>
              <td className="py-2">+{item.xp}</td>
              <td className="py-2">{item.reason}</td>
            </tr>
          ))}
          {xpHistory.length === 0 ? (
            <tr>
              <td colSpan={3} className="py-4 text-center text-muted-foreground">
                Hozircha XP tarixi yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
