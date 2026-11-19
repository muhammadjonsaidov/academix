"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BookOpen, CheckCircle2, ImageIcon, Type } from "lucide-react";
import { DashboardShell } from "@/components/shell/DashboardShell";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import { FileField } from "@/components/shared/FileField";
import { fieldClass } from "@/components/shared/FormField";
import { HOMEWORK_STATUS_META, StatusLabel } from "@/components/shared/submission-status";
import { useStudentStore } from "@/stores/useStudentStore";
import type { StudentHomework, SubmissionType, SubmitHomeworkResponse } from "@/types/student";
import type { ApiErrorResponse } from "@/types/auth";
import { cn } from "@/lib/utils";
import { deadlineLabel, formatDateTime } from "@/lib/format";

// A photo of the notebook page is the usual way to hand in work, so it comes first.
const TYPE_OPTIONS: { value: SubmissionType; label: string; icon: typeof Type }[] = [
  { value: "IMAGE", label: "Daftar surati", icon: ImageIcon },
  { value: "TEXT", label: "Matn", icon: Type },
  { value: "MIXED", label: "Surat + matn", icon: BookOpen },
];

function SubmitForm({ assignmentId, onDone }: { assignmentId: string; onDone: () => void }) {
  const submitHomework = useStudentStore((state) => state.submitHomework);
  const [type, setType] = useState<SubmissionType>("IMAGE");
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
      <div className="flex flex-wrap gap-2">
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
                  ? "border-foreground bg-foreground text-background"
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
          className={cn(fieldClass, "h-auto w-full py-2")}
        />
      )}
      {(type === "IMAGE" || type === "MIXED") && (
        <FileField
          id={`submit-image-${assignmentId}`}
          accept="image/jpeg,image/png"
          onChange={(e) => setImage(e.target.files?.[0] ?? null)}
          fileName={image?.name}
          hint="Varaqni yorug' joyda, to'liq sig'adigan qilib suratga oling (JPG yoki PNG)"
          buttonLabel="Surat tanlash"
        />
      )}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Yuborilmoqda…" : "Ishni yuborish"}
      </Button>
    </form>
  );
}

function HomeworkRow({ hw }: { hw: StudentHomework }) {
  const [isOpen, setIsOpen] = useState(false);
  const meta = HOMEWORK_STATUS_META[hw.submissionStatus];
  const pending = hw.submissionStatus === "PENDING";
  const due = deadlineLabel(hw.deadlineAt);
  const overdue = due === "muddati o'tgan";

  return (
    <li className="px-4 py-4 sm:px-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0 space-y-1">
          <p className="font-medium">{hw.title}</p>
          <p className="text-sm text-muted-foreground">
            {hw.subject} ·{" "}
            {pending ? (
              <span className={cn(overdue ? "font-medium text-pen-red" : "")}>
                {due === "muddati o'tgan" ? "Muddati o'tgan — hali ham topshirsa bo'ladi" : due}
              </span>
            ) : (
              <span className="font-data">{formatDateTime(hw.deadlineAt)}</span>
            )}
          </p>
          {hw.myTask?.taskContent ? (
            <p className="max-w-2xl pt-1 text-[0.9375rem]">{hw.myTask.taskContent}</p>
          ) : null}
        </div>
        <div className="flex shrink-0 items-center gap-2">
          {pending ? (
            <Button size="sm" variant={isOpen ? "outline" : "default"} onClick={() => setIsOpen((v) => !v)}>
              {isOpen ? "Yopish" : "Topshirish"}
            </Button>
          ) : (
            <Badge variant={meta.badgeVariant}>
              <StatusLabel status={hw.submissionStatus}>{meta.label}</StatusLabel>
            </Badge>
          )}
        </div>
      </div>
      {isOpen ? <SubmitForm assignmentId={hw.assignmentId} onDone={() => {}} /> : null}
    </li>
  );
}

export default function StudentHomeworkPage() {
  const homework = useStudentStore((state) => state.homework);
  const fetchHomework = useStudentStore((state) => state.fetchHomework);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchHomework()
      .catch(() => setError("Vazifalarni yuklab bo'lmadi. Sahifani yangilang."))
      .finally(() => setIsLoading(false));
  }, [fetchHomework]);

  const pending = homework
    .filter((hw) => hw.submissionStatus === "PENDING")
    .sort((a, b) => new Date(a.deadlineAt).getTime() - new Date(b.deadlineAt).getTime());
  const done = homework.filter((hw) => hw.submissionStatus !== "PENDING");

  return (
    <DashboardShell role="STUDENT">
      <div className="space-y-8">
        <PageHeader
          title="Vazifalar"
          description={
            isLoading
              ? "Vazifalar yuklanmoqda…"
              : pending.length > 0
                ? `${pending.length} ta vazifani topshirish kerak. Eng yaqin muddatlisi tepada.`
                : "Hamma vazifalar topshirilgan."
          }
        />

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} className="h-20" />
            ))}
          </div>
        ) : homework.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="Hozircha vazifa yo'q"
            description="O'qituvchingiz yangi vazifa berganda shu yerda va Telegram'da ko'rasiz."
          />
        ) : (
          <>
            {pending.length > 0 ? (
              <section aria-labelledby="pending-heading" className="space-y-3">
                <h2 id="pending-heading" className="text-lg font-semibold">
                  Topshirish kerak
                </h2>
                <ul className="divide-y divide-border overflow-hidden rounded-[10px] border border-border bg-card">
                  {pending.map((hw) => (
                    <HomeworkRow key={hw.assignmentId} hw={hw} />
                  ))}
                </ul>
              </section>
            ) : null}
            {done.length > 0 ? (
              <section aria-labelledby="done-heading" className="space-y-3">
                <h2 id="done-heading" className="text-lg font-semibold">
                  Topshirilganlar
                </h2>
                <ul className="divide-y divide-border overflow-hidden rounded-[10px] border border-border bg-card">
                  {done.map((hw) => (
                    <HomeworkRow key={hw.assignmentId} hw={hw} />
                  ))}
                </ul>
              </section>
            ) : null}
          </>
        )}
      </div>
    </DashboardShell>
  );
}
