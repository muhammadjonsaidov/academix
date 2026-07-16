"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { useStudentStore } from "@/stores/useStudentStore";

export default function StudentBadgesPage() {
  const badges = useStudentStore((state) => state.badges);
  const fetchBadges = useStudentStore((state) => state.fetchBadges);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchBadges().catch(() => setError("Yutuqlarni yuklab bo'lmadi."));
  }, [fetchBadges]);

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">Yutuqlarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
        {badges.map((badge) => (
          <div key={badge.id} className="space-y-1 rounded-md border border-border p-4 text-center">
            <p className="text-3xl">{badge.icon}</p>
            <p className="font-medium">{badge.name}</p>
            <p className="text-sm text-muted-foreground">{badge.description}</p>
            <p className="text-xs text-muted-foreground">
              {new Date(badge.awardedAt).toLocaleDateString()}
            </p>
          </div>
        ))}
      </div>
      {badges.length === 0 ? (
        <p className="text-sm text-muted-foreground">Hozircha yutuqlar yo&apos;q.</p>
      ) : null}
    </DashboardShell>
  );
}
