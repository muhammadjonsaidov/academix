"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, FileCheck2 } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { FULL_STATUS_META, StatusLabel } from "@/components/shared/submission-status";
import { useStudentStore } from "@/stores/useStudentStore";
import { cn } from "@/lib/utils";

export default function StudentSubmissionsPage() {
  const submissions = useStudentStore((state) => state.submissions);
  const fetchSubmissions = useStudentStore((state) => state.fetchSubmissions);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchSubmissions()
      .catch(() => setError("Topshiriqlarni yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchSubmissions]);

  return (
    <DashboardShell role="STUDENT">
      <h2 className="mb-4 font-heading text-lg font-semibold">Topshirilgan ishlarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16" />
          ))}
        </div>
      ) : submissions.length === 0 ? (
        <EmptyState
          icon={FileCheck2}
          title="Hozircha topshiriqlar yo'q"
          description="Vazifa topshirganingizda natijasi shu yerda ko'rinadi."
        />
      ) : (
        <div className="space-y-3">
          {submissions.map((s) => {
            const meta = FULL_STATUS_META[s.status];
            return (
              <Link key={s.submissionId} href={`/dashboard/student/submissions/${s.submissionId}`}>
                <Card className={cn("card-lift transition-colors hover:bg-muted/40", meta.rail || undefined)}>
                  <CardContent className="flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <span className="font-data text-sm text-muted-foreground">
                        {new Date(s.submittedAt).toLocaleString()}
                      </span>
                      {s.isLate ? <Badge variant="severity-medium">Kechikkan</Badge> : null}
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant={meta.badgeVariant}>
                        <StatusLabel status={s.status}>{meta.label}</StatusLabel>
                      </Badge>
                      <ChevronRight className="size-4 text-muted-foreground" strokeWidth={1.75} />
                    </div>
                  </CardContent>
                </Card>
              </Link>
            );
          })}
        </div>
      )}
    </DashboardShell>
  );
}
