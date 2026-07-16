"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { ParentNav } from "@/components/parent/ParentNav";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Button } from "@/components/ui/button";
import { useConsentStore } from "@/stores/useConsentStore";
import { useParentStore } from "@/stores/useParentStore";
import type { ApiErrorResponse } from "@/types/auth";

const TREND_LABEL: Record<string, string> = { UP: "↑ O'sish", DOWN: "↓ Pasayish", STABLE: "→ Barqaror" };

function trendClass(trend: string) {
  if (trend === "UP") return "text-green-600";
  if (trend === "DOWN") return "text-destructive";
  return "text-muted-foreground";
}

export default function ParentChildDetailPage() {
  const params = useParams<{ studentId: string }>();
  const studentId = params.studentId;

  const overview = useParentStore((state) => state.selectedOverview);
  const progress = useParentStore((state) => state.selectedProgress);
  const grades = useParentStore((state) => state.selectedGrades);
  const homework = useParentStore((state) => state.selectedHomework);
  const fetchOverview = useParentStore((state) => state.fetchOverview);
  const fetchProgress = useParentStore((state) => state.fetchProgress);
  const fetchGrades = useParentStore((state) => state.fetchGrades);
  const fetchHomework = useParentStore((state) => state.fetchHomework);
  const requestDataDeletion = useConsentStore((state) => state.requestDataDeletion);

  const [error, setError] = useState<string | null>(null);
  const [deletionMessage, setDeletionMessage] = useState<string | null>(null);
  const [isRequestingDeletion, setIsRequestingDeletion] = useState(false);

  useEffect(() => {
    fetchOverview(studentId).catch(() => setError("Ma'lumotlarni yuklab bo'lmadi."));
    fetchProgress(studentId).catch(() => {});
    fetchGrades(studentId).catch(() => {});
    fetchHomework(studentId).catch(() => {});
  }, [studentId, fetchOverview, fetchProgress, fetchGrades, fetchHomework]);

  async function handleDataDeletionRequest() {
    setError(null);
    setDeletionMessage(null);
    setIsRequestingDeletion(true);
    try {
      await requestDataDeletion(studentId);
      setDeletionMessage("So'rov yuborildi. Administrator tasdiqlashini kuting.");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "So'rov yuborib bo'lmadi.");
    } finally {
      setIsRequestingDeletion(false);
    }
  }

  if (!overview) {
    return (
      <DashboardShell role="PARENT">
        <ParentNav />
        {error ? <p className="text-sm text-destructive">{error}</p> : <p>Yuklanmoqda...</p>}
      </DashboardShell>
    );
  }

  return (
    <DashboardShell role="PARENT">
      <ParentNav />
      <h2 className="mb-1 text-lg font-semibold">
        {overview.summary.name} — {overview.summary.className}
      </h2>
      <p className="mb-6 text-sm text-muted-foreground">
        Bugungi faollik: {overview.summary.todayActivity ? "Ha" : "Yo'q"} · Bajarilmagan
        vazifalar: {overview.summary.pendingHomeworkCount}
      </p>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {progress ? (
        <div className="mb-6 space-y-3 rounded-md border border-border p-4">
          <p className="font-medium">Fanlar bo&apos;yicha rivojlanish</p>
          <p className="text-xs text-muted-foreground">
            Sinf o&apos;rtachasi yoki boshqa o&apos;quvchilar bilan solishtirish ko&apos;rsatilmaydi.
          </p>
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground">
              <tr>
                <th className="py-1">Fan</th>
                <th className="py-1">Joriy o&apos;rtacha</th>
                <th className="py-1">O&apos;tgan oy</th>
                <th className="py-1">Topshirish darajasi</th>
                <th className="py-1">Tendensiya</th>
              </tr>
            </thead>
            <tbody>
              {progress.subjectProgress.map((sp) => (
                <tr key={sp.subject} className="border-t border-border">
                  <td className="py-1">{sp.subject}</td>
                  <td className="py-1">{sp.currentAvg.toFixed(1)}</td>
                  <td className="py-1">{sp.previousMonthAvg.toFixed(1)}</td>
                  <td className="py-1">{Math.round(sp.submissionRate * 100)}%</td>
                  <td className={`py-1 ${trendClass(sp.trend)}`}>{TREND_LABEL[sp.trend]}</td>
                </tr>
              ))}
              {progress.subjectProgress.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-2 text-center text-muted-foreground">
                    Ma&apos;lumot yo&apos;q.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>

          {progress.badges.length > 0 ? (
            <div>
              <p className="mb-1 text-sm font-medium">Yutuqlar</p>
              <div className="flex flex-wrap gap-2 text-sm">
                {progress.badges.map((b) => (
                  <span key={b.name} className="rounded-md border border-border px-2 py-1">
                    {b.icon} {b.name}
                  </span>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}

      {grades ? (
        <div className="mb-6 space-y-3 rounded-md border border-border p-4">
          <p className="font-medium">Baholar</p>
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground">
              <tr>
                <th className="py-1">Fan</th>
                <th className="py-1">Ball</th>
                <th className="py-1">Baho</th>
              </tr>
            </thead>
            <tbody>
              {grades.recent.map((g) => (
                <tr key={g.submissionId} className="border-t border-border">
                  <td className="py-1">{g.subject}</td>
                  <td className="py-1">{g.score}</td>
                  <td className="py-1">{g.fivePointGrade}</td>
                </tr>
              ))}
              {grades.recent.length === 0 ? (
                <tr>
                  <td colSpan={3} className="py-2 text-center text-muted-foreground">
                    Hozircha baholar yo&apos;q.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      ) : null}

      <div className="mb-6 space-y-3 rounded-md border border-border p-4">
        <p className="font-medium">Bajarilmagan vazifalar</p>
        <ul className="space-y-1 text-sm">
          {homework
            .filter((h) => h.submissionStatus === "PENDING")
            .map((h) => (
              <li key={h.assignmentId}>
                {h.subjectName} — {h.title} ({new Date(h.deadlineAt).toLocaleDateString()})
              </li>
            ))}
          {homework.filter((h) => h.submissionStatus === "PENDING").length === 0 ? (
            <li className="text-muted-foreground">Bajarilmagan vazifalar yo&apos;q.</li>
          ) : null}
        </ul>
      </div>

      <div className="space-y-2 rounded-md border border-border p-4">
        <p className="font-medium">Ma&apos;lumotlarni o&apos;chirish so&apos;rovi</p>
        <p className="text-xs text-muted-foreground">
          Faqat o&apos;quvchi maktabni tark etgandan so&apos;ng yuborish mumkin.
        </p>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={isRequestingDeletion}
          onClick={handleDataDeletionRequest}
        >
          {isRequestingDeletion ? "..." : "So'rov yuborish"}
        </Button>
        {deletionMessage ? <p className="text-sm text-green-600">{deletionMessage}</p> : null}
      </div>
    </DashboardShell>
  );
}
