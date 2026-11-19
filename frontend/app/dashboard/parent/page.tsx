"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, Users } from "lucide-react";
import { DashboardHero } from "@/components/shared/DashboardHero";
import { DashboardShell } from "@/components/shell/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { GradeMark } from "@/components/shared/GradeMark";
import { TelegramConnect } from "@/components/shared/TelegramConnect";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthStore } from "@/stores/useAuthStore";
import { useParentStore } from "@/stores/useParentStore";
import type { ChildSummary } from "@/types/parent";

function summarySentence(children: ChildSummary[]): string {
  const pending = children.reduce((sum, c) => sum + c.pendingHomeworkCount, 0);
  if (children.length === 0) return "Farzandingiz maktab tizimiga bog'langach, uning baholari shu yerda chiqadi.";
  if (pending === 0) return "Hamma vazifalar topshirilgan. Yangi baho kelsa, Telegram'ga ham xabar boradi.";
  return `${pending} ta vazifa hali topshirilmagan. Yangi baho kelsa, Telegram'ga ham xabar boradi.`;
}

export default function ParentDashboardPage() {
  const dashboard = useParentStore((state) => state.dashboard);
  const fetchDashboard = useParentStore((state) => state.fetchDashboard);
  const firstName = useAuthStore((state) => state.user?.firstName);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Ma'lumotni yuklab bo'lmadi. Sahifani yangilang."));
  }, [fetchDashboard]);

  const children = dashboard?.children ?? [];

  return (
    <DashboardShell role="PARENT">
      <div className="space-y-8">
        <DashboardHero
          title={firstName ? `Salom, ${firstName}` : "Salom"}
          description={dashboard ? summarySentence(children) : "Farzandlaringiz natijalari yuklanmoqda…"}
        />

        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : null}

        {!dashboard && !error ? (
          <div className="grid gap-4 md:grid-cols-2">
            <Skeleton className="h-36" />
            <Skeleton className="h-36" />
          </div>
        ) : null}

        {dashboard && children.length === 0 ? (
          <EmptyState
            icon={Users}
            title="Bog'langan farzand yo'q"
            description="Maktab ma'muriyatidan farzandingizni hisobingizga bog'lab berishni so'rang."
          />
        ) : null}

        {children.length > 0 ? (
          <ul className="grid gap-4 md:grid-cols-2">
            {children.map((child) => (
              <li key={child.studentId}>
                <Link
                  href={`/dashboard/parent/children/${child.studentId}`}
                  className="card-lift group flex h-full items-start gap-4 rounded-[10px] border border-border bg-card p-5"
                >
                  <div className="min-w-0 flex-1 space-y-3">
                    <div>
                      <p className="truncate text-lg font-semibold">{child.name}</p>
                      <p className="text-sm text-muted-foreground">{child.className}</p>
                    </div>
                    <dl className="space-y-1 text-[0.9375rem]">
                      <div className="flex gap-2">
                        <dt className="text-muted-foreground">So&apos;nggi baho:</dt>
                        <dd>
                          {child.recentGrade ? (
                            <span className="font-data">{child.recentGrade.score} ball</span>
                          ) : (
                            "hali yo'q"
                          )}
                        </dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="text-muted-foreground">Topshirilmagan:</dt>
                        <dd className={child.pendingHomeworkCount > 0 ? "font-medium" : ""}>
                          {child.pendingHomeworkCount > 0 ? `${child.pendingHomeworkCount} ta vazifa` : "yo'q"}
                        </dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="text-muted-foreground">Bugun:</dt>
                        <dd>{child.todayActivity ? "tizimda ishlagan" : "kirmagan"}</dd>
                      </div>
                    </dl>
                  </div>
                  <div className="flex shrink-0 flex-col items-end gap-3">
                    {child.recentGrade ? <GradeMark grade={child.recentGrade.fivePointGrade} /> : null}
                    <ChevronRight
                      className="size-4 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                      strokeWidth={1.75}
                    />
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        ) : null}

        {dashboard ? (
          <section aria-labelledby="telegram-heading" className="max-w-xl space-y-2">
            <h2 id="telegram-heading" className="text-lg font-semibold">
              Baholarni Telegram&apos;da olish
            </h2>
            <TelegramConnect />
          </section>
        ) : null}
      </div>
    </DashboardShell>
  );
}
