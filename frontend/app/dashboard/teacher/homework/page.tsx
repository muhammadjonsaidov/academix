"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { BookOpen } from "lucide-react";
import { DashboardShell } from "@/components/shell/DashboardShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField, SelectField } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { Homework } from "@/types/teacher";
import { formatDateTime } from "@/lib/format";

// LocalDateTime from the backend ("2026-08-01T10:00:00") -> value a <input type="datetime-local">
// accepts ("2026-08-01T10:00").
function toDatetimeLocalValue(isoLike: string) {
  return isoLike.slice(0, 16);
}

export default function TeacherHomeworkPage() {
  const classes = useTeacherStore((state) => state.classes);
  const subjects = useTeacherStore((state) => state.subjects);
  const homework = useTeacherStore((state) => state.homework);
  const fetchClasses = useTeacherStore((state) => state.fetchClasses);
  const fetchSubjects = useTeacherStore((state) => state.fetchSubjects);
  const fetchHomework = useTeacherStore((state) => state.fetchHomework);
  const createHomework = useTeacherStore((state) => state.createHomework);
  const updateHomework = useTeacherStore((state) => state.updateHomework);
  const deleteHomework = useTeacherStore((state) => state.deleteHomework);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [deadlineAt, setDeadlineAt] = useState("");
  const [maxScore, setMaxScore] = useState("100");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  useEffect(() => {
    fetchClasses().catch(() => {});
    fetchSubjects().catch(() => {});
    fetchHomework().catch(() => setError("Uy vazifalarini yuklab bo'lmadi."));
  }, [fetchClasses, fetchSubjects, fetchHomework]);

  function classFullName(id: string) {
    return classes.find((c) => c.id === id)?.fullName ?? "—";
  }

  function subjectName(id: string) {
    return subjects.find((s) => s.id === id)?.name ?? "—";
  }

  function resetForm() {
    setShowForm(false);
    setEditingId(null);
    setClassId("");
    setSubjectId("");
    setTitle("");
    setDescription("");
    setDeadlineAt("");
    setMaxScore("100");
  }

  function startEdit(hw: Homework) {
    setError(null);
    setNotice(null);
    setShowForm(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
    setEditingId(hw.id);
    setClassId(hw.classId);
    setSubjectId(hw.subjectId);
    setTitle(hw.title);
    setDescription(hw.description ?? "");
    setDeadlineAt(toDatetimeLocalValue(hw.deadlineAt));
    setMaxScore(String(hw.maxScore));
  }

  async function handleDelete(hw: Homework) {
    // Real safety net is server-side (HomeworkService.delete rejects if any submission exists,
    // ERR_HW_HAS_SUBMISSIONS) — this dialog is just a normal are-you-sure, not the actual guard.
    const confirmed = window.confirm(`"${hw.title}" vazifasini o'chirmoqchimisiz?`);
    if (!confirmed) return;
    setError(null);
    setDeletingId(hw.id);
    try {
      await deleteHomework(hw.id);
      if (editingId === hw.id) {
        resetForm();
      }
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Uy vazifasini o'chirib bo'lmadi.");
    } finally {
      setDeletingId(null);
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      if (editingId) {
        await updateHomework(editingId, {
          title,
          description,
          deadlineAt,
          maxScore: Number(maxScore),
        });
        resetForm();
        setNotice("Vazifa saqlandi.");
      } else {
        await createHomework({
          classId,
          subjectId,
          title,
          description,
          deadlineAt,
          maxScore: Number(maxScore),
        });
        resetForm();
        setNotice(`"${title}" berildi. Sinf o'quvchilariga xabar yuborildi.`);
      }
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(
        apiError?.message ??
          (editingId ? "Uy vazifasini yangilab bo'lmadi." : "Uy vazifasi yaratib bo'lmadi."),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <PageHeader
        className="mb-6"
        title="Uy vazifalari"
        description="Vazifa bering — o'quvchilar Telegram'da xabar oladi, topshirilgan ishlarni AI tekshiradi."
        actions={
          !showForm ? (
            <Button
              onClick={() => {
                setNotice(null);
                setShowForm(true);
              }}
            >
              Yangi vazifa berish
            </Button>
          ) : null
        }
      />

      {notice ? (
        <p role="status" className="mb-4 text-sm text-success">
          {notice}
        </p>
      ) : null}

      {showForm ? (
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>{editingId ? "Vazifani tahrirlash" : "Yangi vazifa"}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <FormField label="Sinf" htmlFor="classId">
              <SelectField
                id="classId"
                value={classId}
                onChange={(e) => setClassId(e.target.value)}
                required
                disabled={!!editingId}
              >
                <option value="" disabled>
                  Tanlang
                </option>
                {classes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.fullName}
                  </option>
                ))}
              </SelectField>
            </FormField>
            <FormField label="Fan" htmlFor="subjectId">
              <SelectField
                id="subjectId"
                value={subjectId}
                onChange={(e) => setSubjectId(e.target.value)}
                required
                disabled={!!editingId}
              >
                <option value="" disabled>
                  Tanlang
                </option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </SelectField>
            </FormField>
            <FormField label="Sarlavha" htmlFor="title" className="sm:col-span-2">
              <input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <FormField label="Tavsif" htmlFor="description" className="sm:col-span-2">
              <input
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <FormField label="Muddat" htmlFor="deadlineAt">
              <input
                id="deadlineAt"
                type="datetime-local"
                value={deadlineAt}
                onChange={(e) => setDeadlineAt(e.target.value)}
                required
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <FormField label="Maksimal ball" htmlFor="maxScore">
              <input
                id="maxScore"
                type="number"
                min={1}
                value={maxScore}
                onChange={(e) => setMaxScore(e.target.value)}
                required
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <div className="flex flex-wrap items-end gap-3 sm:col-span-2 lg:col-span-4">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting
                  ? editingId
                    ? "Saqlanmoqda..."
                    : "Yaratilmoqda..."
                  : editingId
                    ? "Saqlash"
                    : "Vazifani berish"}
              </Button>
              <Button type="button" variant="ghost" onClick={resetForm} disabled={isSubmitting}>
                Bekor qilish
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      ) : null}

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {homework.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="Hozircha uy vazifalari yo'q"
          description="«Yangi vazifa berish» tugmasi bilan birinchi vazifani bering."
        />
      ) : (
        <Card>
          <CardContent className="px-0">
            <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="px-6 py-2">Sarlavha</th>
                  <th className="py-2">Sinf</th>
                  <th className="py-2">Fan</th>
                  <th className="py-2">Muddat</th>
                  <th className="px-6 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {[...homework]
                  .sort((a, b) => new Date(b.deadlineAt).getTime() - new Date(a.deadlineAt).getTime())
                  .map((hw) => (
                  <tr key={hw.id} className="border-b border-border last:border-0">
                    <td className="px-6 py-3 font-medium">{hw.title}</td>
                    <td className="py-3">{classFullName(hw.classId)}</td>
                    <td className="py-3">{subjectName(hw.subjectId)}</td>
                    <td className="py-3 font-data">{formatDateTime(hw.deadlineAt)}</td>
                    <td className="space-x-3 px-6 py-3 text-right">
                      <Link
                        href={`/dashboard/teacher/submissions?assignmentId=${hw.id}`}
                        className="text-sm underline"
                      >
                        Ishlar
                      </Link>
                      <button
                        type="button"
                        onClick={() => startEdit(hw)}
                        className="text-sm underline"
                      >
                        Tahrirlash
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(hw)}
                        disabled={deletingId === hw.id}
                        className="text-sm text-destructive underline disabled:opacity-60"
                      >
                        {deletingId === hw.id ? "O'chirilmoqda..." : "O'chirish"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          </CardContent>
        </Card>
      )}
    </DashboardShell>
  );
}
