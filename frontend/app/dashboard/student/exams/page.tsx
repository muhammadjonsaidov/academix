"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, ClipboardList } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/student/EmptyState";
import { useExamStore } from "@/stores/useExamStore";
import { cn } from "@/lib/utils";

export default function StudentExamsPage() {
  const studentExams = useExamStore((state) => state.studentExams);
  const fetchStudentExams = useExamStore((state) => state.fetchStudentExams);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchStudentExams()
      .catch(() => setError("Nazorat ishlarini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchStudentExams]);

  return (
    <DashboardShell role="STUDENT">
      <h2 className="mb-4 font-heading text-lg font-semibold">Nazorat ishlarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16" />
          ))}
        </div>
      ) : studentExams.length === 0 ? (
        <EmptyState
          icon={ClipboardList}
          title="Hozircha nazorat ishlari yo'q"
          description="O'qituvchingiz nazorat ishi natijasini kiritganda shu yerda ko'rinadi."
        />
      ) : (
        <div className="space-y-3">
          {studentExams.map((exam) => (
            <Link key={exam.examId} href={`/dashboard/student/exams/${exam.examId}`}>
              <Card
                className={cn("transition-colors hover:bg-muted/40", exam.myGrade && "rail-verified")}
              >
                <CardContent className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-medium">{exam.title}</p>
                    <p className="text-sm text-muted-foreground">
                      {exam.subject} · {exam.examDate}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {exam.myGrade ? (
                      <Badge variant="ready" className="font-data">
                        {exam.myGrade.score} ({exam.myGrade.fivePointGrade})
                      </Badge>
                    ) : (
                      <Badge variant="outline">Kutilmoqda</Badge>
                    )}
                    <ChevronRight className="size-4 text-muted-foreground" strokeWidth={1.75} />
                  </div>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </DashboardShell>
  );
}
