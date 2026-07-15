"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { Button } from "@/components/ui/button";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function TeacherHomeworkPage() {
  const classes = useTeacherStore((state) => state.classes);
  const subjects = useTeacherStore((state) => state.subjects);
  const homework = useTeacherStore((state) => state.homework);
  const fetchClasses = useTeacherStore((state) => state.fetchClasses);
  const fetchSubjects = useTeacherStore((state) => state.fetchSubjects);
  const fetchHomework = useTeacherStore((state) => state.fetchHomework);
  const createHomework = useTeacherStore((state) => state.createHomework);

  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [deadlineAt, setDeadlineAt] = useState("");
  const [maxScore, setMaxScore] = useState("100");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

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

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await createHomework({
        classId,
        subjectId,
        title,
        description,
        deadlineAt,
        type: "STANDARD",
        maxScore: Number(maxScore),
      });
      setTitle("");
      setDescription("");
      setDeadlineAt("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Uy vazifasi yaratib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />
      <h2 className="mb-4 text-lg font-semibold">Uy vazifalari</h2>

      <form onSubmit={handleSubmit} className="mb-6 flex flex-wrap items-end gap-3">
        <div className="space-y-1">
          <label htmlFor="classId" className="text-sm font-medium">
            Sinf
          </label>
          <select
            id="classId"
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          >
            <option value="" disabled>
              Tanlang
            </option>
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.fullName}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <label htmlFor="subjectId" className="text-sm font-medium">
            Fan
          </label>
          <select
            id="subjectId"
            value={subjectId}
            onChange={(e) => setSubjectId(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          >
            <option value="" disabled>
              Tanlang
            </option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1">
          <label htmlFor="title" className="text-sm font-medium">
            Sarlavha
          </label>
          <input
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="description" className="text-sm font-medium">
            Tavsif
          </label>
          <input
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="deadlineAt" className="text-sm font-medium">
            Muddat
          </label>
          <input
            id="deadlineAt"
            type="datetime-local"
            value={deadlineAt}
            onChange={(e) => setDeadlineAt(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="maxScore" className="text-sm font-medium">
            Maksimal ball
          </label>
          <input
            id="maxScore"
            type="number"
            min={1}
            value={maxScore}
            onChange={(e) => setMaxScore(e.target.value)}
            required
            className="w-24 rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Yaratilmoqda..." : "Vazifa yaratish"}
        </Button>
      </form>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Sarlavha</th>
            <th className="py-2">Sinf</th>
            <th className="py-2">Fan</th>
            <th className="py-2">Muddat</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {homework.map((hw) => (
            <tr key={hw.id} className="border-b border-border">
              <td className="py-2 font-medium">{hw.title}</td>
              <td className="py-2">{classFullName(hw.classId)}</td>
              <td className="py-2">{subjectName(hw.subjectId)}</td>
              <td className="py-2">{new Date(hw.deadlineAt).toLocaleString()}</td>
              <td className="py-2 text-right">
                <Link
                  href={`/dashboard/teacher/submissions?assignmentId=${hw.id}`}
                  className="text-sm underline"
                >
                  Topshiriqlar
                </Link>
              </td>
            </tr>
          ))}
          {homework.length === 0 ? (
            <tr>
              <td colSpan={5} className="py-4 text-center text-muted-foreground">
                Hozircha uy vazifalari yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
