"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { Button } from "@/components/ui/button";
import { useStudentStore } from "@/stores/useStudentStore";
import type { SubmissionType } from "@/types/student";
import type { ApiErrorResponse } from "@/types/auth";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Topshirilmagan",
  SUBMITTED: "Topshirilgan",
  GRADED: "Baholangan",
};

function SubmitForm({ assignmentId, onDone }: { assignmentId: string; onDone: () => void }) {
  const submitHomework = useStudentStore((state) => state.submitHomework);
  const [type, setType] = useState<SubmissionType>("TEXT");
  const [textContent, setTextContent] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [result, setResult] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const response = await submitHomework(assignmentId, type, textContent, image);
      setResult(response.message);
      onDone();
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Topshirib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (result) {
    return <p className="text-sm text-muted-foreground">{result}</p>;
  }

  return (
    <form onSubmit={handleSubmit} className="mt-2 space-y-2 border-t border-border pt-2">
      <div className="flex gap-3">
        {(["TEXT", "IMAGE", "MIXED"] as SubmissionType[]).map((t) => (
          <label key={t} className="flex items-center gap-1 text-sm">
            <input
              type="radio"
              name={`type-${assignmentId}`}
              checked={type === t}
              onChange={() => setType(t)}
            />
            {t}
          </label>
        ))}
      </div>
      {(type === "TEXT" || type === "MIXED") && (
        <textarea
          value={textContent}
          onChange={(e) => setTextContent(e.target.value)}
          placeholder="Yechimingizni yozing..."
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
        />
      )}
      {(type === "IMAGE" || type === "MIXED") && (
        <input
          type="file"
          accept="image/jpeg,image/png"
          onChange={(e) => setImage(e.target.files?.[0] ?? null)}
          className="text-sm"
        />
      )}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Topshirilmoqda..." : "Topshirish"}
      </Button>
    </form>
  );
}

export default function StudentHomeworkPage() {
  const homework = useStudentStore((state) => state.homework);
  const fetchHomework = useStudentStore((state) => state.fetchHomework);
  const [error, setError] = useState<string | null>(null);
  const [openAssignmentId, setOpenAssignmentId] = useState<string | null>(null);

  useEffect(() => {
    fetchHomework().catch(() => setError("Vazifalarni yuklab bo'lmadi."));
  }, [fetchHomework]);

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">Vazifalar</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <ul className="space-y-4">
        {homework.map((hw) => (
          <li key={hw.assignmentId} className="rounded-md border border-border p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">{hw.title}</p>
                <p className="text-sm text-muted-foreground">
                  {hw.subject} · Muddat: {new Date(hw.deadlineAt).toLocaleString()}
                  {hw.isLate ? " · Kechikkan" : ""}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-sm">{STATUS_LABEL[hw.submissionStatus]}</span>
                {hw.submissionStatus === "PENDING" ? (
                  <Button
                    variant="outline"
                    onClick={() =>
                      setOpenAssignmentId(
                        openAssignmentId === hw.assignmentId ? null : hw.assignmentId,
                      )
                    }
                  >
                    Topshirish
                  </Button>
                ) : null}
              </div>
            </div>
            {openAssignmentId === hw.assignmentId ? (
              <SubmitForm
                assignmentId={hw.assignmentId}
                onDone={() => setOpenAssignmentId(null)}
              />
            ) : null}
          </li>
        ))}
        {homework.length === 0 ? (
          <p className="text-sm text-muted-foreground">Hozircha vazifalar yo&apos;q.</p>
        ) : null}
      </ul>
    </DashboardShell>
  );
}
