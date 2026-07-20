"use client";

import { useEffect, useState } from "react";
import { History, Sparkles } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/student/EmptyState";
import { useStudentStore } from "@/stores/useStudentStore";

export default function StudentXpHistoryPage() {
  const xpHistory = useStudentStore((state) => state.xpHistory);
  const fetchXpHistory = useStudentStore((state) => state.fetchXpHistory);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchXpHistory()
      .catch(() => setError("XP tarixini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchXpHistory]);

  return (
    <DashboardShell role="STUDENT">
      <h2 className="mb-4 flex items-center gap-2 font-heading text-lg font-semibold">
        <History className="size-5 text-role-student" strokeWidth={1.75} />
        XP tarixi
      </h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {isLoading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </div>
      ) : xpHistory.length === 0 ? (
        <EmptyState
          icon={Sparkles}
          title="Hozircha XP tarixi yo'q"
          description="Vazifalarni topshirib, birinchi XP'ingizni qo'lga kiriting."
        />
      ) : (
        <Card>
          <CardContent className="divide-y divide-border p-0">
            {xpHistory.map((item, i) => (
              <div key={i} className="flex items-center justify-between gap-3 px-6 py-3 text-sm">
                <div className="min-w-0">
                  <p className="truncate font-medium">{item.reason}</p>
                  <p className="font-data text-xs text-muted-foreground">
                    {new Date(item.date).toLocaleString()}
                  </p>
                </div>
                <span className="font-data shrink-0 text-base font-semibold text-role-student">
                  +{item.xp} XP
                </span>
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </DashboardShell>
  );
}
