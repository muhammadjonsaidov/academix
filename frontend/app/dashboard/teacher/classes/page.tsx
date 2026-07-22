"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, School, Users } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTeacherStore } from "@/stores/useTeacherStore";

// Real data source: GET /teacher/classes (academix_tz.md §2.3), already wired via
// useTeacherStore.fetchClasses/classes — reused by the homework/exam/syllabus create forms.
// This page just gives that same list a standalone browse entry point (the "Sinflar" nav
// slot the shell-stage agent flagged as missing), linking each row into the existing
// classes/[classId]/analytics drill-in rather than inventing new data or a new endpoint.
export default function TeacherClassesPage() {
  const classes = useTeacherStore((state) => state.classes);
  const fetchClasses = useTeacherStore((state) => state.fetchClasses);
  const [error, setError] = useState<string | null>(null);
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    fetchClasses()
      .catch(() => setError("Sinflar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setHasLoaded(true));
  }, [fetchClasses]);

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-4 font-heading text-lg font-semibold">Sinflarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {!hasLoaded ? (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-20" />
          ))}
        </div>
      ) : classes.length === 0 ? (
        <EmptyState
          icon={School}
          title="Sinflar topilmadi"
          description="Sizga hali biror sinf/fan biriktirilmagan. Administrator bilan bog'laning."
        />
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {classes.map((c) => (
            <Link key={c.id} href={`/dashboard/teacher/classes/${c.id}/analytics`}>
              <Card className="card-lift h-full transition-colors hover:bg-muted/40">
                <CardContent className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-role-teacher-muted text-role-teacher">
                      <School className="size-4.5" strokeWidth={1.75} />
                    </span>
                    <div>
                      <p className="font-medium">{c.fullName}</p>
                      <p className="flex items-center gap-1 text-xs text-muted-foreground">
                        <Users className="size-3" strokeWidth={1.75} />
                        <span className="font-data">{c.studentCount}</span> o&apos;quvchi
                      </p>
                    </div>
                  </div>
                  <ChevronRight className="size-4 shrink-0 text-muted-foreground" strokeWidth={1.75} />
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
