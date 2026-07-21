"use client";

import { useEffect, useState, type FormEvent } from "react";
import { NotebookPen, Sparkles } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
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
            className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            rows={3}
          />
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
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
          <form onSubmit={handleGenerate} className="flex flex-wrap items-end gap-3">
            <div className="space-y-1">
              <label htmlFor="syllabusId" className="text-sm font-medium">
                Darslik
              </label>
              <select
                id="syllabusId"
                value={syllabusId}
                onChange={(e) => setSyllabusId(e.target.value)}
                required
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              >
                <option value="" disabled>
                  Tanlang
                </option>
                {syllabuses.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.title}
                  </option>
                ))}
              </select>
            </div>
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
              <label htmlFor="topic" className="text-sm font-medium">
                Mavzu
              </label>
              <input
                id="topic"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                required
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </div>
            <div className="space-y-1">
              <label htmlFor="lessonDate" className="text-sm font-medium">
                Dars sanasi
              </label>
              <input
                id="lessonDate"
                type="date"
                value={lessonDate}
                onChange={(e) => setLessonDate(e.target.value)}
                required
                className="rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </div>
            <Button type="submit" disabled={isGenerating}>
              {isGenerating ? "Generatsiya qilinmoqda..." : "Reja generatsiya qilish"}
            </Button>
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
