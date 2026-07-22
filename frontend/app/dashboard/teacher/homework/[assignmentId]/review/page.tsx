"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AlertTriangle, CheckCircle2, Sparkles } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { UniqueTask } from "@/types/teacher";

function UniqueTaskRow({ assignmentId, task }: { assignmentId: string; task: UniqueTask }) {
  const approveUniqueTask = useTeacherStore((state) => state.approveUniqueTask);
  const editUniqueTask = useTeacherStore((state) => state.editUniqueTask);
  const [content, setContent] = useState(task.taskContent);
  const [isSaving, setIsSaving] = useState(false);
  const [isApproving, setIsApproving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSaveContent() {
    setError(null);
    setIsSaving(true);
    try {
      await editUniqueTask(assignmentId, task.taskId, content);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Saqlab bo'lmadi.");
    } finally {
      setIsSaving(false);
    }
  }

  async function handleApprove() {
    setError(null);
    setIsApproving(true);
    try {
      await approveUniqueTask(assignmentId, task.taskId);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Tasdiqlab bo'lmadi.");
    } finally {
      setIsApproving(false);
    }
  }

  const railClass = task.flaggedForReview ? "rail-flagged" : task.isApproved ? "rail-verified" : "";

  return (
    <Card className={railClass}>
      <CardContent className="space-y-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="font-medium">{task.studentName}</p>
          <div className="flex items-center gap-2">
            {task.flaggedForReview ? (
              <Badge variant="flagged">
                <AlertTriangle className="size-3" strokeWidth={1.75} />
                Ko&apos;rib chiqish kerak
              </Badge>
            ) : null}
            {task.fallbackToStandard ? (
              <Badge variant="outline">Standart topshiriqqa tushirildi</Badge>
            ) : null}
            {task.isApproved ? (
              <Badge variant="ready">
                <CheckCircle2 className="size-3" strokeWidth={1.75} />
                Tasdiqlangan
              </Badge>
            ) : null}
          </div>
        </div>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
          rows={2}
        />
        {error ? <p className="text-sm text-destructive">{error}</p> : null}
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={handleSaveContent} disabled={isSaving}>
            {isSaving ? "Saqlanmoqda..." : "Matnni saqlash"}
          </Button>
          <Button onClick={handleApprove} disabled={isApproving || task.isApproved}>
            {task.isApproved ? "Tasdiqlangan" : isApproving ? "Tasdiqlanmoqda..." : "Tasdiqlash"}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export default function UniqueTaskReviewPage() {
  const params = useParams<{ assignmentId: string }>();
  const assignmentId = params.assignmentId;
  const router = useRouter();

  const uniqueTasks = useTeacherStore((state) => state.uniqueTasks);
  const homework = useTeacherStore((state) => state.homework);
  const fetchUniqueTasks = useTeacherStore((state) => state.fetchUniqueTasks);
  const fetchHomework = useTeacherStore((state) => state.fetchHomework);
  const approveAllUniqueTasks = useTeacherStore((state) => state.approveAllUniqueTasks);
  const submitHomework = useTeacherStore((state) => state.submitHomework);

  const [error, setError] = useState<string | null>(null);
  const [isApprovingAll, setIsApprovingAll] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    fetchHomework().catch(() => {});
  }, [fetchHomework]);

  useEffect(() => {
    fetchUniqueTasks(assignmentId)
      .catch(() => setError("Topshiriqlarni yuklab bo'lmadi."))
      .finally(() => setHasLoaded(true));
  }, [fetchUniqueTasks, assignmentId]);

  // `tasksPublished` lives on the parent Homework record, not on the individual UniqueTask —
  // used to tell "generation still running" apart from "genuinely nothing was generated."
  const assignment = homework.find((hw) => hw.id === assignmentId);

  const flaggedTasks = useMemo(() => uniqueTasks.filter((t) => t.flaggedForReview), [uniqueTasks]);
  const readyTasks = useMemo(() => uniqueTasks.filter((t) => !t.flaggedForReview), [uniqueTasks]);

  async function handleApproveAll() {
    setError(null);
    setIsApprovingAll(true);
    try {
      // Backend contract (academix_tz.md §2.3): approve-all only ever touches
      // flaggedForReview=false rows server-side — flagged rows always require individual
      // review via approveUniqueTask, never bulk.
      await approveAllUniqueTasks(assignmentId);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Ommaviy tasdiqlab bo'lmadi.");
    } finally {
      setIsApprovingAll(false);
    }
  }

  async function handleSubmit() {
    setError(null);
    setIsSubmitting(true);
    try {
      await submitHomework(assignmentId);
      router.push("/dashboard/teacher/homework");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "O'quvchilarga yuborib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  const isGenerating = hasLoaded && assignment ? !assignment.tasksPublished && uniqueTasks.length === 0 : false;

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-1 font-heading text-lg font-semibold">
        Unique topshiriqlarni ko&apos;rib chiqish
      </h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Har bir o&apos;quvchi uchun generatsiya qilingan topshiriqni tekshiring va tasdiqlang.
        Belgilangan (flagged) topshiriqlar alohida ko&apos;rib chiqilishi shart.
      </p>

      {!hasLoaded ? (
        <div className="space-y-3">
          <Skeleton className="h-10 w-80" />
          <Skeleton className="h-28" />
          <Skeleton className="h-28" />
        </div>
      ) : null}

      {hasLoaded && isGenerating ? (
        <EmptyState
          icon={Sparkles}
          title="Generatsiya qilinmoqda..."
          description="AI har bir o'quvchi uchun individual topshiriq yaratmoqda. Bir necha daqiqadan so'ng sahifani qayta yuklang."
        />
      ) : null}

      {hasLoaded && !isGenerating ? (
        <>
          {uniqueTasks.length > 0 ? (
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-md border border-border bg-muted/40 px-4 py-3 text-sm">
              <span>
                {uniqueTasks.length} ta o&apos;quvchidan{" "}
                <span className="font-data font-semibold">{flaggedTasks.length}</span> tasi ko&apos;rib
                chiqishni talab qiladi.
              </span>
              <div className="flex flex-wrap gap-3">
                <Button
                  variant="outline"
                  onClick={handleApproveAll}
                  disabled={isApprovingAll || readyTasks.every((t) => t.isApproved)}
                >
                  {isApprovingAll ? "Tasdiqlanmoqda..." : "Belgilanmaganlarni ommaviy tasdiqlash"}
                </Button>
                <Button onClick={handleSubmit} disabled={isSubmitting}>
                  {isSubmitting ? "Yuborilmoqda..." : "O'quvchilarga yuborish"}
                </Button>
              </div>
            </div>
          ) : null}

          {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

          {uniqueTasks.length === 0 ? (
            <EmptyState
              icon={Sparkles}
              title="Hozircha topshiriqlar yo'q"
              description="Bu vazifa uchun individual topshiriqlar generatsiya qilinmagan."
            />
          ) : (
            <div className="space-y-6">
              {flaggedTasks.length > 0 ? (
                <div className="space-y-3">
                  <h3 className="text-sm font-semibold text-severity-medium">
                    Ko&apos;rib chiqish kerak ({flaggedTasks.length})
                  </h3>
                  <div className="space-y-3">
                    {flaggedTasks.map((task) => (
                      <UniqueTaskRow key={task.taskId} assignmentId={assignmentId} task={task} />
                    ))}
                  </div>
                </div>
              ) : null}

              <div className="space-y-3">
                <h3 className="text-sm font-semibold text-muted-foreground">
                  Tayyor ({readyTasks.length})
                </h3>
                <div className="space-y-3">
                  {readyTasks.map((task) => (
                    <UniqueTaskRow key={task.taskId} assignmentId={assignmentId} task={task} />
                  ))}
                  {readyTasks.length === 0 ? (
                    <p className="text-sm text-muted-foreground">
                      Barcha topshiriqlar ko&apos;rib chiqishni talab qiladi.
                    </p>
                  ) : null}
                </div>
              </div>
            </div>
          )}
        </>
      ) : null}
    </DashboardShell>
  );
}
