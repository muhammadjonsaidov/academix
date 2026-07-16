"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { Button } from "@/components/ui/button";
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

  return (
    <li className="space-y-2 rounded-md border border-border p-4">
      <div className="flex items-center justify-between">
        <p className="font-medium">{task.studentName}</p>
        <div className="flex items-center gap-2 text-sm">
          {task.flaggedForReview ? (
            <span className="text-destructive">Ko&apos;rib chiqish talab qilinadi</span>
          ) : null}
          {task.fallbackToStandard ? (
            <span className="text-muted-foreground">Standart topshiriqqa tushirildi</span>
          ) : null}
          {task.isApproved ? (
            <span className="text-muted-foreground">Tasdiqlangan</span>
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
      <div className="flex gap-2">
        <Button variant="outline" onClick={handleSaveContent} disabled={isSaving}>
          {isSaving ? "Saqlanmoqda..." : "Matnni saqlash"}
        </Button>
        <Button onClick={handleApprove} disabled={isApproving || task.isApproved}>
          {task.isApproved ? "Tasdiqlangan" : isApproving ? "Tasdiqlanmoqda..." : "Tasdiqlash"}
        </Button>
      </div>
    </li>
  );
}

export default function UniqueTaskReviewPage() {
  const params = useParams<{ assignmentId: string }>();
  const assignmentId = params.assignmentId;
  const router = useRouter();

  const uniqueTasks = useTeacherStore((state) => state.uniqueTasks);
  const fetchUniqueTasks = useTeacherStore((state) => state.fetchUniqueTasks);
  const approveAllUniqueTasks = useTeacherStore((state) => state.approveAllUniqueTasks);
  const submitHomework = useTeacherStore((state) => state.submitHomework);

  const [error, setError] = useState<string | null>(null);
  const [isApprovingAll, setIsApprovingAll] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchUniqueTasks(assignmentId).catch(() => setError("Topshiriqlarni yuklab bo'lmadi."));
  }, [fetchUniqueTasks, assignmentId]);

  async function handleApproveAll() {
    setError(null);
    setIsApprovingAll(true);
    try {
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

  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />
      <h2 className="mb-1 text-lg font-semibold">Unique topshiriqlarni ko&apos;rib chiqish</h2>
      <p className="mb-4 text-sm text-muted-foreground">
        Har bir o&apos;quvchi uchun generatsiya qilingan topshiriqni tekshiring va tasdiqlang.
        Belgilangan (flagged) topshiriqlar alohida ko&apos;rib chiqilishi shart.
      </p>

      <div className="mb-4 flex gap-3">
        <Button variant="outline" onClick={handleApproveAll} disabled={isApprovingAll}>
          {isApprovingAll ? "Tasdiqlanmoqda..." : "Belgilanmaganlarni ommaviy tasdiqlash"}
        </Button>
        <Button onClick={handleSubmit} disabled={isSubmitting}>
          {isSubmitting ? "Yuborilmoqda..." : "O'quvchilarga yuborish"}
        </Button>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <ul className="space-y-3">
        {uniqueTasks.map((task) => (
          <UniqueTaskRow key={task.taskId} assignmentId={assignmentId} task={task} />
        ))}
        {uniqueTasks.length === 0 ? (
          <p className="text-sm text-muted-foreground">Hozircha topshiriqlar yo&apos;q.</p>
        ) : null}
      </ul>
    </DashboardShell>
  );
}
