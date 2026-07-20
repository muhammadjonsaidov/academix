"use client";

import { useEffect, useState } from "react";
import { Trophy } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/student/EmptyState";
import { useStudentStore } from "@/stores/useStudentStore";

export default function StudentBadgesPage() {
  const badges = useStudentStore((state) => state.badges);
  const fetchBadges = useStudentStore((state) => state.fetchBadges);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchBadges()
      .catch(() => setError("Yutuqlarni yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchBadges]);

  return (
    <DashboardShell role="STUDENT">
      <h2 className="mb-4 font-heading text-lg font-semibold">Yutuqlarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {isLoading ? (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-36" />
          ))}
        </div>
      ) : badges.length === 0 ? (
        <EmptyState
          icon={Trophy}
          title="Hozircha yutuqlar yo'q"
          description="Vazifalarni bajarib, birinchi yutug'ingizni qo'lga kiriting."
        />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
          {badges.map((badge) => (
            <Card key={badge.id} className="text-center">
              <CardContent className="flex flex-col items-center gap-1.5">
                <span className="flex size-14 items-center justify-center rounded-full bg-role-student-muted text-3xl">
                  {badge.icon}
                </span>
                <p className="font-medium">{badge.name}</p>
                <p className="text-sm text-muted-foreground">{badge.description}</p>
                <p className="font-data text-xs text-muted-foreground">
                  {new Date(badge.awardedAt).toLocaleDateString()}
                </p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
