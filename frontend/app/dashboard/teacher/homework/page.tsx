"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { BookOpen, Sparkles } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField, SelectField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { AssignmentType, Homework } from "@/types/teacher";

// LocalDateTime from the backend ("2026-08-01T10:00:00") -> value a <input type="datetime-local">
// accepts ("2026-08-01T10:00").
function toDatetimeLocalValue(isoLike: string) {
  return isoLike.slice(0, 16);
}

export default function TeacherHomeworkPage() {
  const router = useRouter();
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
  const [type, setType] = useState<AssignmentType>("STANDARD");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

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
    setEditingId(null);
    setClassId("");
    setSubjectId("");
    setTitle("");
    setDescription("");
    setDeadlineAt("");
    setMaxScore("100");
    setType("STANDARD");
  }

  function startEdit(hw: Homework) {
    setError(null);
    setEditingId(hw.id);
    setClassId(hw.classId);
    setSubjectId(hw.subjectId);
    setTitle(hw.title);
    setDescription(hw.description ?? "");
    setDeadlineAt(toDatetimeLocalValue(hw.deadlineAt));
    setMaxScore(String(hw.maxScore));
    setType(hw.type);
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
      } else {
        const created = await createHomework({
          classId,
          subjectId,
          title,
          description,
          deadlineAt,
          type,
          maxScore: Number(maxScore),
        });
        setTitle("");
        setDescription("");
        setDeadlineAt("");
        if (type === "UNIQUE_GENERATED") {
          router.push(`/dashboard/teacher/homework/${created.id}/review`);
        }
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
      <h2 className="mb-4 font-heading text-lg font-semibold">Uy vazifalari</h2>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>{editingId ? "Vazifani tahrirlash" : "Yangi vazifa yaratish"}</CardTitle>
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
            <div className="flex min-w-0 flex-col gap-1.5 sm:col-span-2">
              <span className="text-sm font-medium">Turi</span>
              <div className="flex h-9 items-center gap-4">
                <label className="flex items-center gap-1.5 text-sm">
                  <input
                    type="radio"
                    name="type"
                    className="accent-[var(--ink)]"
                    checked={type === "STANDARD"}
                    disabled={!!editingId}
                    onChange={() => setType("STANDARD")}
                  />
                  Standart
                </label>
                <label className="flex items-center gap-1.5 text-sm">
                  <input
                    type="radio"
                    name="type"
                    className="accent-[var(--ink)]"
                    checked={type === "UNIQUE_GENERATED"}
                    disabled={!!editingId}
                    onChange={() => setType("UNIQUE_GENERATED")}
                  />
                  Har biriga unique
                </label>
              </div>
            </div>
            <div className="flex flex-wrap items-end gap-3 sm:col-span-2 lg:col-span-4">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting
                  ? editingId
                    ? "Saqlanmoqda..."
                    : "Yaratilmoqda..."
                  : editingId
                    ? "Saqlash"
                    : "Vazifa yaratish"}
              </Button>
              {editingId ? (
                <Button type="button" variant="ghost" onClick={resetForm} disabled={isSubmitting}>
                  Bekor qilish
                </Button>
              ) : null}
            </div>
          </form>
        </CardContent>
      </Card>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {homework.length === 0 ? (
        <EmptyState
          icon={BookOpen}
          title="Hozircha uy vazifalari yo'q"
          description="Yuqoridagi forma orqali birinchi vazifangizni yarating."
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
                  <th className="py-2">Turi</th>
                  <th className="px-6 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {homework.map((hw) => (
                  <tr key={hw.id} className="border-b border-border last:border-0">
                    <td className="px-6 py-3 font-medium">{hw.title}</td>
                    <td className="py-3">{classFullName(hw.classId)}</td>
                    <td className="py-3">{subjectName(hw.subjectId)}</td>
                    <td className="py-3 font-data">{new Date(hw.deadlineAt).toLocaleString()}</td>
                    <td className="py-3">
                      {hw.type === "UNIQUE_GENERATED" ? (
                        <div className="flex items-center gap-1.5">
                          <Badge variant="outline">Unique</Badge>
                          {!hw.tasksPublished ? (
                            <Badge variant="status-processing" className="gap-1">
                              <Sparkles className="size-3" strokeWidth={1.75} />
                              Generatsiya
                            </Badge>
                          ) : null}
                        </div>
                      ) : (
                        <Badge variant="outline">Standart</Badge>
                      )}
                    </td>
                    <td className="space-x-3 px-6 py-3 text-right">
                      {hw.type === "UNIQUE_GENERATED" && !hw.tasksPublished ? (
                        <Link
                          href={`/dashboard/teacher/homework/${hw.id}/review`}
                          className="text-sm underline"
                        >
                          Ko&apos;rib chiqish
                        </Link>
                      ) : null}
                      <Link
                        href={`/dashboard/teacher/submissions?assignmentId=${hw.id}`}
                        className="text-sm underline"
                      >
                        Topshiriqlar
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
