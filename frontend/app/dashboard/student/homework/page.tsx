"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BookOpen, CheckCircle2, ImageIcon, Type } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/student/EmptyState";
import { HOMEWORK_STATUS_META } from "@/components/student/submission-status";
import { useStudentStore } from "@/stores/useStudentStore";
import type { StudentHomework, SubmissionType, SubmitHomeworkResponse } from "@/types/student";
import type { ApiErrorResponse } from "@/types/auth";
import { cn } from "@/lib/utils";

const TYPE_OPTIONS: { value: SubmissionType; label: string; icon: typeof Type }[] = [
  { value: "TEXT", label: "Matn", icon: Type },
  { value: "IMAGE", label: "Rasm", icon: ImageIcon },
  { value: "MIXED", label: "Matn + rasm", icon: BookOpen },
];

function SubmitForm({ assignmentId, onDone }: { assignmentId: string; onDone: () => void }) {
  const submitHomework = useStudentStore((state) => state.submitHomework);
  const [type, setType] = useState<SubmissionType>("TEXT");
  const [textContent, setTextContent] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<SubmitHomeworkResponse | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const response = await submitHomework(assignmentId, type, textContent, image);
      setResult(response);
      onDone();
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Topshirib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (result) {
    return (
      <div className="mt-3 flex flex-col items-start gap-2 border-t border-border pt-3">
        <div className="flex items-center gap-2 text-sm text-success">
          <CheckCircle2 className="size-4" strokeWidth={1.75} />
          <span>{result.message}</span>
        </div>
        <Button
          size="sm"
          render={<Link href={`/dashboard/student/submissions/${result.submissionId}`} />}
        >
          Natijani ko&apos;rish
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="mt-3 space-y-3 border-t border-border pt-3">
      <div className="flex gap-2">
        {TYPE_OPTIONS.map((opt) => {
          const Icon = opt.icon;
          const active = type === opt.value;
          return (
            <button
              key={opt.value}
              type="button"
              onClick={() => setType(opt.value)}
              className={cn(
                "flex items-center gap-1.5 rounded-md border px-2.5 py-1.5 text-xs font-medium transition-colors",
                active
                  ? "border-role-student bg-role-student-muted text-role-student"
                  : "border-border text-muted-foreground hover:bg-muted/60",
              )}
            >
              <Icon className="size-3.5" strokeWidth={1.75} />
              {opt.label}
            </button>
          );
        })}
      </div>
      {(type === "TEXT" || type === "MIXED") && (
        <textarea
          value={textContent}
          onChange={(e) => setTextContent(e.target.value)}
          placeholder="Yechimingizni yozing..."
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        />
      )}
      {(type === "IMAGE" || type === "MIXED") && (
        <input
          type="file"
          accept="image/jpeg,image/png"
          onChange={(e) => setImage(e.target.files?.[0] ?? null)}
          className="text-sm"
        />
      )}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Topshirilmoqda..." : "Topshirish"}
      </Button>
    </form>
  );
}

function HomeworkCard({ hw }: { hw: StudentHomework }) {
  const [isOpen, setIsOpen] = useState(false);
  const meta = HOMEWORK_STATUS_META[hw.submissionStatus];

  return (
    <Card className={cn(meta.rail || undefined)}>
      <CardContent>
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="font-medium">{hw.title}</p>
            <p className="text-sm text-muted-foreground">
              {hw.subject} · Muddat:{" "}
              <span className="font-data">{new Date(hw.deadlineAt).toLocaleString()}</span>
              {hw.isLate ? (
                <Badge variant="severity-medium" className="ml-2">
                  Kechikkan
                </Badge>
              ) : null}
            </p>
            {hw.myTask ? (
              <p className="mt-2 rounded-md bg-muted/50 p-2 text-sm">
                <span className="font-medium">Sizning topshirig&apos;ingiz: </span>
                {hw.myTask.taskContent}
              </p>
            ) : null}
          </div>
          <div className="flex shrink-0 flex-col items-end gap-2">
            <Badge variant={meta.badgeVariant}>{meta.label}</Badge>
            {hw.submissionStatus === "PENDING" ? (
              <Button variant="outline" size="sm" onClick={() => setIsOpen((v) => !v)}>
                Topshirish
              </Button>
            ) : null}
          </div>
        </div>
        {isOpen ? <SubmitForm assignmentId={hw.assignmentId} onDone={() => {}} /> : null}
      </CardContent>
    </Card>
  );
}

export default function StudentHomeworkPage() {
  const homework = useStudentStore((state) => state.homework);
  const fetchHomework = useStudentStore((state) => state.fetchHomework);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchHomework()
      .catch(() => setError("Vazifalarni yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchHomework]);

  return (
    <DashboardShell role="STUDENT">
      <h2 className="mb-4 font-heading text-lg font-semibold">Vazifalar</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {isLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      ) : (
        <div className="space-y-4">
          {homework.map((hw) => (
            <HomeworkCard key={hw.assignmentId} hw={hw} />
          ))}
          {homework.length === 0 ? (
            <EmptyState
              icon={BookOpen}
              title="Hozircha vazifalar yo'q"
              description="O'qituvchingiz yangi vazifa berganda shu yerda ko'rinadi."
            />
          ) : null}
        </div>
      )}
    </DashboardShell>
  );
}
