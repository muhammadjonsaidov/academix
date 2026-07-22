"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { ClipboardList } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useExamStore } from "@/stores/useExamStore";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function TeacherExamsPage() {
  const classes = useTeacherStore((state) => state.classes);
  const subjects = useTeacherStore((state) => state.subjects);
  const fetchClasses = useTeacherStore((state) => state.fetchClasses);
  const fetchSubjects = useTeacherStore((state) => state.fetchSubjects);
  const exams = useExamStore((state) => state.exams);
  const fetchExams = useExamStore((state) => state.fetchExams);
  const createExam = useExamStore((state) => state.createExam);

  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [title, setTitle] = useState("");
  const [examDate, setExamDate] = useState("");
  const [maxScore, setMaxScore] = useState("100");
  const [error, setError] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchClasses().catch(() => {});
    fetchSubjects().catch(() => {});
    fetchExams().catch(() => setError("Imtihonlar ro'yxatini yuklab bo'lmadi."));
  }, [fetchClasses, fetchSubjects, fetchExams]);

  function classFullName(id: string) {
    return classes.find((c) => c.id === id)?.fullName ?? "—";
  }

  function subjectName(id: string) {
    return subjects.find((s) => s.id === id)?.name ?? "—";
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setWarning(null);
    setIsSubmitting(true);
    try {
      const created = await createExam({ classId, subjectId, title, examDate, maxScore: Number(maxScore) });
      setTitle("");
      setExamDate("");
      if (created.warning) {
        setWarning(
          `${created.warning} (taxminan ${created.estimatedAiCalls} ta chaqiruv, ${created.remainingExamBudget} ta qoldi)`,
        );
      }
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Imtihon yaratib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-4 font-heading text-lg font-semibold">Nazorat ishlari</h2>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Yangi imtihon yaratish</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
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
              <label htmlFor="examDate" className="text-sm font-medium">
                Sana
              </label>
              <input
                id="examDate"
                type="date"
                value={examDate}
                onChange={(e) => setExamDate(e.target.value)}
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
              {isSubmitting ? "Yaratilmoqda..." : "Imtihon yaratish"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {warning ? <p className="mb-4 text-sm text-severity-medium">{warning}</p> : null}
      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {exams.length === 0 ? (
        <EmptyState
          icon={ClipboardList}
          title="Hozircha imtihonlar yo'q"
          description="Yuqoridagi forma orqali birinchi nazorat ishingizni yarating."
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
                  <th className="py-2">Sana</th>
                  <th className="py-2">Topshirilgan / Baholangan</th>
                  <th className="px-6 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {exams.map((exam) => (
                  <tr key={exam.examId} className="border-b border-border last:border-0">
                    <td className="px-6 py-3 font-medium">{exam.title}</td>
                    <td className="py-3">{classFullName(exam.classId)}</td>
                    <td className="py-3">{subjectName(exam.subjectId)}</td>
                    <td className="py-3 font-data">{exam.examDate}</td>
                    <td className="py-3 font-data">
                      {exam.submissionsCount} / {exam.gradedCount}
                    </td>
                    <td className="px-6 py-3 text-right">
                      <Link href={`/dashboard/teacher/exams/${exam.examId}`} className="text-sm underline">
                        Ko&apos;rish
                      </Link>
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
