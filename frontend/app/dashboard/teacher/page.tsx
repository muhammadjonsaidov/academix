"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BookOpen, CheckCheck, ClipboardCheck } from "lucide-react";
import { DashboardHero, HeroAction } from "@/components/shared/DashboardHero";
import { DashboardShell } from "@/components/shell/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { ProgressBar } from "@/components/shared/ProgressBar";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { formatDate } from "@/lib/format";
import { useAuthStore } from "@/stores/useAuthStore";
import { useTeacherAnalyticsStore } from "@/stores/useTeacherAnalyticsStore";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ReviewQueueItem } from "@/types/teacher";

/** "14 ta ish sizni kutmoqda, 3 tasi diqqat talab qiladi." — the one sentence that matters. */
function queueSentence(queue: ReviewQueueItem[]): string {
  const waiting = queue.reduce((sum, item) => sum + item.waiting, 0);
  const flagged = queue.reduce((sum, item) => sum + item.flagged, 0);
  if (waiting === 0) return "Hamma ishlar tekshirilgan. Yangi ishlar kelganda shu yerda paydo bo'ladi.";
  const base = `${waiting} ta ish sizni kutmoqda`;
  return flagged > 0 ? `${base}, ${flagged} tasi diqqat talab qiladi.` : `${base}.`;
}

export default function TeacherDashboardPage() {
  const firstName = useAuthStore((state) => state.user?.firstName);
  const dashboard = useTeacherAnalyticsStore((state) => state.dashboard);
  const fetchDashboard = useTeacherAnalyticsStore((state) => state.fetchDashboard);
  const queue = useTeacherStore((state) => state.reviewQueue);
  const fetchReviewQueue = useTeacherStore((state) => state.fetchReviewQueue);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([fetchReviewQueue(), fetchDashboard()]).catch(() =>
      setError("Bosh sahifani yuklab bo'lmadi. Sahifani yangilang."),
    );
  }, [fetchReviewQueue, fetchDashboard]);

  return (
    <DashboardShell role="TEACHER">
      <div className="space-y-8">
        <DashboardHero
          title={firstName ? `Salom, ${firstName}` : "Salom"}
          description={queue ? queueSentence(queue) : "Tekshirish navbati yuklanmoqda…"}
          actions={
            <>
              <HeroAction href="/dashboard/teacher/homework" icon={BookOpen}>
                Yangi vazifa berish
              </HeroAction>
              <HeroAction href="/dashboard/teacher/exams" icon={ClipboardCheck}>
                Nazorat ishi
              </HeroAction>
            </>
          }
        />

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        <section aria-labelledby="queue-heading" className="space-y-3">
          <div className="flex items-baseline justify-between gap-3">
            <h2 id="queue-heading" className="text-lg font-semibold">
              Tekshirish navbati
            </h2>
            <Link
              href="/dashboard/teacher/submissions"
              className="text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
            >
              Barcha ishlar
            </Link>
          </div>
          {queue === null && !error ? <QueueSkeleton /> : null}
          {queue && queue.length === 0 ? (
            <EmptyState
              icon={CheckCheck}
              title="Navbat bo'sh"
              description="AI tekshirgan ishlar shu yerga tushadi. Yangi vazifa bering — o'quvchilar topshirgach, AI ularni bir necha daqiqada tekshiradi."
            />
          ) : null}
          {queue && queue.length > 0 ? (
            <ul className="divide-y divide-border overflow-hidden rounded-[10px] border border-border bg-card">
              {queue.map((item) => (
                <QueueRow key={item.assignmentId} item={item} />
              ))}
            </ul>
          ) : null}
        </section>

        {dashboard ? (
          <div className="grid gap-8 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
            <section aria-labelledby="today-heading" className="space-y-3">
              <h2 id="today-heading" className="text-lg font-semibold">
                Bugun
              </h2>
              <dl className="space-y-2 text-[0.9375rem]">
                <div className="flex items-baseline justify-between gap-3 border-b border-border pb-2">
                  <dt className="text-muted-foreground">Baholangan ishlar</dt>
                  <dd className="font-data font-semibold">{dashboard.gradedToday}</dd>
                </div>
                <div className="flex items-baseline justify-between gap-3 border-b border-border pb-2">
                  <dt className="text-muted-foreground">Baholanmagan ishlar</dt>
                  <dd className="font-data font-semibold">{dashboard.pendingSubmissions}</dd>
                </div>
                <div className="flex items-baseline justify-between gap-3">
                  <dt className="text-muted-foreground">Sinflarim</dt>
                  <dd className="font-data font-semibold">{dashboard.myClasses.length}</dd>
                </div>
              </dl>
            </section>

            <section aria-labelledby="classes-heading" className="space-y-3">
              <h2 id="classes-heading" className="text-lg font-semibold">
                Sinflar o&apos;zlashtirishi
              </h2>
              {dashboard.classProgressSummary.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Birinchi ishlar baholangach, har bir sinfning o&apos;rtacha natijasi shu yerda chiqadi.
                </p>
              ) : (
                <ul className="divide-y divide-border">
                  {dashboard.classProgressSummary.map((c) => (
                    <li key={c.classId}>
                      <Link
                        href={`/dashboard/teacher/classes/${c.classId}/analytics`}
                        className="grid grid-cols-[5rem_minmax(0,1fr)_3rem] items-center gap-3 py-2.5 text-sm"
                      >
                        <span className="truncate font-medium">{c.className}</span>
                        <ProgressBar
                          value={c.avgScore}
                          tone={c.avgScore >= 70 ? "success" : c.avgScore >= 50 ? "role" : "destructive"}
                        />
                        <span className="font-data text-right text-muted-foreground">{Math.round(c.avgScore)}%</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        ) : null}
      </div>
    </DashboardShell>
  );
}

function QueueRow({ item }: { item: ReviewQueueItem }) {
  const approveClean = useTeacherStore((state) => state.approveClean);
  const refreshDashboard = useTeacherAnalyticsStore((state) => state.fetchDashboard);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const clean = item.waiting - item.flagged;

  async function handleApprove() {
    setBusy(true);
    setMessage(null);
    try {
      const approved = await approveClean(item.assignmentId);
      setMessage(`${approved} ta ish baholandi.`);
      refreshDashboard().catch(() => {});
    } catch {
      setMessage("Tasdiqlab bo'lmadi. Qayta urinib ko'ring.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <li className="flex flex-col gap-3 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-5">
      <div className="min-w-0 space-y-1">
        <p className="truncate font-medium">{item.assignmentTitle}</p>
        <p className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
          <span className="font-data">{item.waiting} ta ish</span>
          {item.flagged > 0 ? (
            <span className="marker text-xs font-medium">
              {item.flagged} tasi shubhali
            </span>
          ) : null}
          {item.oldestSubmittedAt ? <span>eng eskisi {formatDate(item.oldestSubmittedAt)}</span> : null}
        </p>
        {message ? (
          <p role="status" className="text-sm text-success">
            {message}
          </p>
        ) : null}
      </div>
      <div className="flex shrink-0 flex-wrap gap-2 sm:justify-end">
        {clean > 0 ? (
          <Button size="sm" variant="outline" onClick={handleApprove} disabled={busy}>
            {busy ? "Tasdiqlanmoqda…" : `${clean} ta toza ishni tasdiqlash`}
          </Button>
        ) : null}
        <Button
          size="sm"
          render={
            <Link
              href={`/dashboard/teacher/submissions?assignmentId=${item.assignmentId}&status=AI_DONE`}
            />
          }
        >
          Tekshirishni boshlash
        </Button>
      </div>
    </li>
  );
}

function QueueSkeleton() {
  return (
    <div className="space-y-px overflow-hidden rounded-[10px] border border-border">
      <Skeleton className="h-20 rounded-none" />
      <Skeleton className="h-20 rounded-none" />
      <Skeleton className="h-20 rounded-none" />
    </div>
  );
}
