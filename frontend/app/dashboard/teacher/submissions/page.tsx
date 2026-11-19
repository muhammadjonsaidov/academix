"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { FileCheck2, RefreshCw } from "lucide-react";
import { DashboardShell } from "@/components/shell/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { PencilScore } from "@/components/shared/GradeMark";
import { PageHeader } from "@/components/shared/PageHeader";
import { PaginationControl } from "@/components/shared/PaginationControl";
import { SubmissionStatusBadge, submissionRailClass } from "@/components/shared/submission-status";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { subscribeToAiStatus } from "@/hooks/useRealtime";
import { formatDateTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { SubmissionStatus, TeacherSubmissionListItem } from "@/types/teacher";

/** Tabs follow the teacher's work, not the pipeline: what needs me, what is still running, what is done. */
const TABS: { status: SubmissionStatus | "ALL"; label: string; empty: string }[] = [
  {
    status: "AI_DONE",
    label: "Tekshirish kerak",
    empty: "AI tekshirgan va sizni kutayotgan ish yo'q.",
  },
  {
    status: "AI_SKIPPED",
    label: "Qo'lda baholash",
    empty: "Qo'lda baholanadigan ish yo'q. AI limiti tugaganda ishlar shu yerga tushadi.",
  },
  {
    status: "AI_PROCESSING",
    label: "AI tekshirmoqda",
    empty: "Hozir AI tekshirayotgan ish yo'q.",
  },
  { status: "GRADED", label: "Baholangan", empty: "Hali baholangan ish yo'q." },
  { status: "ALL", label: "Hammasi", empty: "O'quvchilar vazifani topshirganda shu yerda ko'rinadi." },
];

function isFlagged(item: TeacherSubmissionListItem): boolean {
  const type = item.aiFeedback?.plagiarismType;
  return !!type && type !== "CLEAN";
}

export default function TeacherSubmissionsPage() {
  return (
    <Suspense fallback={null}>
      <TeacherSubmissionsList />
    </Suspense>
  );
}

function TeacherSubmissionsList() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const assignmentId = searchParams.get("assignmentId") ?? undefined;
  const tabParam = searchParams.get("status");
  const activeTab = TABS.find((tab) => tab.status === tabParam) ?? TABS[0];

  const submissions = useTeacherStore((state) => state.submissions);
  const fetchSubmissions = useTeacherStore((state) => state.fetchSubmissions);
  const submissionsPage = useTeacherStore((state) => state.submissionsPage);
  const submissionsPageSize = useTeacherStore((state) => state.submissionsPageSize);
  const submissionsTotalItems = useTeacherStore((state) => state.submissionsTotalItems);
  const setSubmissionsPage = useTeacherStore((state) => state.setSubmissionsPage);
  const setSubmissionsPageSize = useTeacherStore((state) => state.setSubmissionsPageSize);
  const [error, setError] = useState<string | null>(null);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const load = useCallback(
    () =>
      fetchSubmissions({
        ...(assignmentId ? { assignmentId } : {}),
        ...(activeTab.status !== "ALL" ? { status: activeTab.status } : {}),
      }),
    [fetchSubmissions, assignmentId, activeTab.status],
  );

  useEffect(() => {
    let cancelled = false;
    load()
      .catch(() => {
        if (!cancelled) setError("Ishlarni yuklab bo'lmadi. Sahifani yangilang.");
      })
      .finally(() => {
        if (!cancelled) setHasLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, [load]);

  // A homework submission finishing AI checking refreshes the list in place.
  useEffect(() => {
    const unsubscribe = subscribeToAiStatus((payload) => {
      if (payload.type === "HOMEWORK" && payload.status !== "AI_PROCESSING") {
        load().catch(() => {});
      }
    });
    return unsubscribe;
  }, [load]);

  function selectTab(status: SubmissionStatus | "ALL") {
    const params = new URLSearchParams(searchParams.toString());
    params.set("status", status);
    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
  }

  async function handleRefresh() {
    setError(null);
    setIsRefreshing(true);
    try {
      await load();
    } catch {
      setError("Ishlarni yuklab bo'lmadi. Sahifani yangilang.");
    } finally {
      setIsRefreshing(false);
    }
  }

  const assignmentTitle = assignmentId ? submissions[0]?.assignmentTitle : null;

  return (
    <DashboardShell role="TEACHER">
      <div className="space-y-5">
        <PageHeader
          title="Topshirilgan ishlar"
          description={
            assignmentId
              ? `"${assignmentTitle ?? "Vazifa"}" bo'yicha ishlar. Shubhali ishlar ro'yxat boshida turadi.`
              : "AI tekshirgan ishlarni ko'rib chiqing: shubhali ishlar ro'yxat boshida turadi."
          }
          actions={
            <>
              {assignmentId ? (
                <Button variant="ghost" size="sm" render={<Link href="/dashboard/teacher/submissions" />}>
                  Barcha vazifalar
                </Button>
              ) : null}
              <Button variant="outline" size="sm" onClick={handleRefresh} disabled={isRefreshing}>
                <RefreshCw className={cn("size-4", isRefreshing && "animate-spin")} strokeWidth={1.75} />
                Yangilash
              </Button>
            </>
          }
        />

        <div role="tablist" aria-label="Ish holati" className="flex gap-1 overflow-x-auto border-b border-border">
          {TABS.map((tab) => {
            const selected = tab.status === activeTab.status;
            return (
              <button
                key={tab.status}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => selectTab(tab.status)}
                className={cn(
                  "-mb-px shrink-0 border-b-2 px-3 py-2 text-sm transition-colors focus-visible:outline-2 focus-visible:outline-ring",
                  selected
                    ? "border-foreground font-semibold text-foreground"
                    : "border-transparent text-muted-foreground hover:text-foreground",
                )}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        {!hasLoaded ? (
          <div className="space-y-2">
            <Skeleton className="h-14" />
            <Skeleton className="h-14" />
            <Skeleton className="h-14" />
          </div>
        ) : submissions.length === 0 ? (
          <EmptyState icon={FileCheck2} title="Bu yerda hozircha ish yo'q" description={activeTab.empty} />
        ) : (
          <>
            <div className="overflow-x-auto rounded-[10px] border border-border bg-card">
              <table className="w-full min-w-[42rem] text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th scope="col" className="px-5 py-2.5 font-medium">
                      O&apos;quvchi
                    </th>
                    <th scope="col" className="py-2.5 font-medium">
                      Vazifa
                    </th>
                    <th scope="col" className="py-2.5 font-medium">
                      Topshirildi
                    </th>
                    <th scope="col" className="py-2.5 font-medium">
                      AI taklifi
                    </th>
                    <th scope="col" className="px-5 py-2.5 text-right font-medium">
                      <span className="sr-only">Amal</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {submissions.map((s) => (
                    <tr key={s.submissionId} className="border-b border-border last:border-0">
                      <td
                        className={cn(
                          "px-5 py-3",
                          isFlagged(s) ? "rail-flagged" : submissionRailClass(s.status),
                        )}
                      >
                        <span className="font-medium">{s.studentName}</span>
                        {s.isLate ? (
                          <span className="ml-2 text-xs text-muted-foreground">kech topshirgan</span>
                        ) : null}
                      </td>
                      <td className="max-w-[16rem] truncate py-3 text-muted-foreground">
                        {s.assignmentTitle ?? "—"}
                      </td>
                      <td className="py-3 font-data whitespace-nowrap text-muted-foreground">
                        {formatDateTime(s.submittedAt)}
                      </td>
                      <td className="py-3">
                        <div className="flex flex-wrap items-center gap-2">
                          {s.aiFeedback ? (
                            <PencilScore percent={s.aiFeedback.aiScorePercent} grade={s.suggestedGrade} />
                          ) : (
                            <SubmissionStatusBadge status={s.status} />
                          )}
                          {isFlagged(s) ? (
                            <span className="marker text-xs font-medium">
                              {s.aiFeedback?.plagiarismType === "AI_GENERATED"
                                ? "AI yozgan bo'lishi mumkin"
                                : "ko'chirilgan bo'lishi mumkin"}
                            </span>
                          ) : null}
                        </div>
                      </td>
                      <td className="px-5 py-3 text-right">
                        <Button
                          size="sm"
                          variant={s.status === "GRADED" ? "ghost" : "outline"}
                          render={<Link href={`/dashboard/teacher/submissions/${s.submissionId}`} />}
                        >
                          {s.status === "GRADED" ? "Ko'rish" : "Tekshirish"}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <PaginationControl
              page={submissionsPage}
              size={submissionsPageSize}
              totalItems={submissionsTotalItems}
              onPageChange={(page) => {
                setSubmissionsPage(page).catch(() => setError("Ishlarni yuklab bo'lmadi."));
              }}
              onSizeChange={(size) => {
                setSubmissionsPageSize(size).catch(() => setError("Ishlarni yuklab bo'lmadi."));
              }}
            />
          </>
        )}
      </div>
    </DashboardShell>
  );
}
