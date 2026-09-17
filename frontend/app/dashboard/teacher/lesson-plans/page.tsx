"use client";

import { useEffect, useState, type FormEvent } from "react";
import { NotebookPen, Sparkles } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField, SelectField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { LessonPlan } from "@/types/teacher";

function LessonPlanCard({ plan }: { plan: LessonPlan }) {
  const updateLessonPlan = useTeacherStore((state) => state.updateLessonPlan);
  const [teacherEditedPlan, setTeacherEditedPlan] = useState(plan.teacherEditedPlan ?? "");
  const [isApproved, setIsApproved] = useState(plan.isApproved);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSave() {
    setError(null);
    setIsSaving(true);
    try {
      await updateLessonPlan(plan.lessonPlanId, { teacherEditedPlan, isApproved });
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Saqlab bo'lmadi.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Card className={plan.isApproved ? "rail-verified" : undefined}>
      <CardContent className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="font-medium">{plan.topic}</p>
          <div className="flex items-center gap-2">
            <span className="font-data text-sm text-muted-foreground">
              {new Date(plan.lessonDate).toLocaleDateString()}
            </span>
            {plan.isApproved ? <Badge variant="ready">Tasdiqlangan</Badge> : null}
          </div>
        </div>

        {plan.aiGeneratedPlan ? (
          <div className="space-y-2 text-sm">
            <div>
              <p className="font-medium">Maqsadlar</p>
              <ul className="list-disc pl-5">
                {plan.aiGeneratedPlan.objectives.map((o) => (
                  <li key={o}>{o}</li>
                ))}
              </ul>
            </div>
            {plan.aiGeneratedPlan.activities.length > 0 ? (
              <div>
                <p className="font-medium">Faoliyatlar</p>
                <ul className="list-disc pl-5">
                  {plan.aiGeneratedPlan.activities.map((a) => (
                    <li key={a.description}>
                      {a.description} ({a.durationMinutes} daq.)
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
            {plan.aiGeneratedPlan.materials.length > 0 ? (
              <p>
                <span className="font-medium">Materiallar: </span>
                {plan.aiGeneratedPlan.materials.join(", ")}
              </p>
            ) : null}
            {plan.aiGeneratedPlan.homeworkSuggestion ? (
              <p>
                <span className="font-medium">Uyga vazifa taklifi: </span>
                {plan.aiGeneratedPlan.homeworkSuggestion}
              </p>
            ) : null}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">AI reja hali mavjud emas.</p>
        )}

        <div className="space-y-2 border-t border-border pt-3">
          <label htmlFor={`edit-${plan.lessonPlanId}`} className="text-sm font-medium">
            Tahrirlangan reja
          </label>
          <textarea
            id={`edit-${plan.lessonPlanId}`}
            value={teacherEditedPlan}
            onChange={(e) => setTeacherEditedPlan(e.target.value)}
            className={`${fieldClass} h-auto w-full py-2`}
            rows={3}
          />
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              className="accent-[var(--ink)]"
              checked={isApproved}
              onChange={(e) => setIsApproved(e.target.checked)}
            />
            Tasdiqlangan
          </label>
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
          <Button variant="outline" onClick={handleSave} disabled={isSaving}>
            {isSaving ? "Saqlanmoqda..." : "Saqlash"}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export default function TeacherLessonPlansPage() {
  const classes = useTeacherStore((state) => state.classes);
  const syllabuses = useTeacherStore((state) => state.syllabuses);
  const lessonPlans = useTeacherStore((state) => state.lessonPlans);
  const fetchClasses = useTeacherStore((state) => state.fetchClasses);
  const fetchSubjects = useTeacherStore((state) => state.fetchSubjects);
  const fetchSyllabuses = useTeacherStore((state) => state.fetchSyllabuses);
  const fetchLessonPlans = useTeacherStore((state) => state.fetchLessonPlans);
  const generateLessonPlan = useTeacherStore((state) => state.generateLessonPlan);

  const [syllabusId, setSyllabusId] = useState("");
  const [classId, setClassId] = useState("");
  const [topic, setTopic] = useState("");
  const [lessonDate, setLessonDate] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  useEffect(() => {
    fetchClasses().catch(() => {});
    fetchSubjects().catch(() => {});
    fetchSyllabuses().catch(() => {});
    fetchLessonPlans().catch(() => setError("Dars rejalarini yuklab bo'lmadi."));
  }, [fetchClasses, fetchSubjects, fetchSyllabuses, fetchLessonPlans]);

  const readySyllabuses = syllabuses.filter((syllabus) => syllabus.processingStatus === "READY");
  const hasPendingIndexing = syllabuses.some(
    (syllabus) =>
      syllabus.processingStatus === "PENDING" || syllabus.processingStatus === "PROCESSING",
  );

  useEffect(() => {
    if (!hasPendingIndexing) return;
    const interval = window.setInterval(() => {
      fetchSyllabuses().catch(() => {});
    }, 4000);
    return () => window.clearInterval(interval);
  }, [fetchSyllabuses, hasPendingIndexing]);

  async function handleGenerate(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsGenerating(true);
    try {
      await generateLessonPlan({ syllabusId, topic, lessonDate, classId });
      setTopic("");
      setLessonDate("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Dars rejasini generatsiya qilib bo'lmadi.");
    } finally {
      setIsGenerating(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-4 font-heading text-lg font-semibold">Dars rejalari</h2>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Sparkles className="size-4 text-muted-foreground" strokeWidth={1.75} />
            AI yordamida reja generatsiya qilish
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleGenerate} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <FormField label="Darslik" htmlFor="syllabusId">
              <SelectField
                id="syllabusId"
                value={syllabusId}
                onChange={(e) => {
                  const selectedId = e.target.value;
                  setSyllabusId(selectedId);
                  const syllabus = readySyllabuses.find((item) => item.id === selectedId);
                  if (syllabus) setClassId(syllabus.classId);
                }}
                required
              >
                <option value="" disabled>
                  Tanlang
                </option>
                {readySyllabuses.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.title}
                  </option>
                ))}
              </SelectField>
            </FormField>
            <FormField label="Sinf" htmlFor="classId">
              <SelectField
                id="classId"
                value={classId}
                onChange={(e) => setClassId(e.target.value)}
                required
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
            <FormField label="Mavzu" htmlFor="topic">
              <input
                id="topic"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                required
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <FormField label="Dars sanasi" htmlFor="lessonDate">
              <input
                id="lessonDate"
                type="date"
                value={lessonDate}
                onChange={(e) => setLessonDate(e.target.value)}
                required
                className={`${fieldClass} w-full`}
              />
            </FormField>
            <div className="flex items-end sm:col-span-2 lg:col-span-4">
              <Button type="submit" disabled={isGenerating || readySyllabuses.length === 0}>
                {isGenerating ? "Generatsiya qilinmoqda..." : "Reja generatsiya qilish"}
              </Button>
              {readySyllabuses.length === 0 ? (
                <p className="ml-3 text-sm text-muted-foreground">
                  Avval AI tayyor holatdagi darslik yuklang.
                </p>
              ) : null}
            </div>
          </form>
        </CardContent>
      </Card>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {lessonPlans.length === 0 ? (
        <EmptyState
          icon={NotebookPen}
          title="Hozircha dars rejalari yo'q"
          description="Yuqoridagi forma orqali AI yordamida birinchi dars rejangizni yarating."
        />
      ) : (
        <ul className="space-y-4">
          {lessonPlans.map((plan) => (
            <li key={plan.lessonPlanId}>
              <LessonPlanCard plan={plan} />
            </li>
          ))}
        </ul>
      )}
    </DashboardShell>
  );
}
