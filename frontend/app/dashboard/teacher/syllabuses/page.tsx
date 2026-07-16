"use client";

import { useEffect, useState, type FormEvent } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { Button } from "@/components/ui/button";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function TeacherSyllabusesPage() {
  const classes = useTeacherStore((state) => state.classes);
  const subjects = useTeacherStore((state) => state.subjects);
  const syllabuses = useTeacherStore((state) => state.syllabuses);
  const fetchClasses = useTeacherStore((state) => state.fetchClasses);
  const fetchSubjects = useTeacherStore((state) => state.fetchSubjects);
  const fetchSyllabuses = useTeacherStore((state) => state.fetchSyllabuses);
  const uploadSyllabus = useTeacherStore((state) => state.uploadSyllabus);

  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchClasses().catch(() => {});
    fetchSubjects().catch(() => {});
    fetchSyllabuses().catch(() => setError("Darsliklarni yuklab bo'lmadi."));
  }, [fetchClasses, fetchSubjects, fetchSyllabuses]);

  function classFullName(id: string) {
    return classes.find((c) => c.id === id)?.fullName ?? "—";
  }

  function subjectName(id: string) {
    return subjects.find((s) => s.id === id)?.name ?? "—";
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!file) {
      setError("Fayl talab qilinadi.");
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await uploadSyllabus(file, subjectId, classId, title);
      setTitle("");
      setFile(null);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Darslik yuklab bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />
      <h2 className="mb-4 text-lg font-semibold">Darsliklar</h2>

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
          <label htmlFor="file" className="text-sm font-medium">
            Fayl (PDF, DOCX, JPG, PNG)
          </label>
          <input
            id="file"
            type="file"
            accept="application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/jpeg,image/png"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            required
            className="text-sm"
          />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Yuklanmoqda..." : "Yuklash"}
        </Button>
      </form>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Sarlavha</th>
            <th className="py-2">Sinf</th>
            <th className="py-2">Fan</th>
            <th className="py-2">Format</th>
            <th className="py-2">Yuklangan</th>
          </tr>
        </thead>
        <tbody>
          {syllabuses.map((s) => (
            <tr key={s.id} className="border-b border-border">
              <td className="py-2 font-medium">{s.title}</td>
              <td className="py-2">{classFullName(s.classId)}</td>
              <td className="py-2">{subjectName(s.subjectId)}</td>
              <td className="py-2">{s.fileType}</td>
              <td className="py-2">{new Date(s.uploadedAt).toLocaleString()}</td>
            </tr>
          ))}
          {syllabuses.length === 0 ? (
            <tr>
              <td colSpan={5} className="py-4 text-center text-muted-foreground">
                Hozircha darsliklar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
