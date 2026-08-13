"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BookOpen, ChevronRight, Flame, Users } from "lucide-react";
import { BiometricConsentBanner } from "@/components/parent/BiometricConsentBanner";
import { DashboardHero } from "@/components/shared/DashboardHero";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StatTile } from "@/components/shared/StatTile";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useConsentStore } from "@/stores/useConsentStore";
import { useParentStore } from "@/stores/useParentStore";
import { useAuthStore } from "@/stores/useAuthStore";

function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .map((part) => part.charAt(0))
    .join("")
    .slice(0, 2)
    .toUpperCase();
}

export default function ParentDashboardPage() {
  const dashboard = useParentStore((state) => state.dashboard);
  const fetchDashboard = useParentStore((state) => state.fetchDashboard);
  const checkPendingConsent = useConsentStore((state) => state.checkPendingConsent);
  const firstName = useAuthStore((state) => state.user?.firstName);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Bosh sahifani yuklab bo'lmadi."));
  }, [fetchDashboard]);

  useEffect(() => {
    if (dashboard) {
      checkPendingConsent(dashboard.children);
    }
  }, [dashboard, checkPendingConsent]);

  const children = dashboard?.children ?? [];
  const activeToday = children.filter((c) => c.todayActivity).length;
  const pendingTotal = children.reduce((sum, c) => sum + c.pendingHomeworkCount, 0);

  const heroDescription = dashboard
    ? `${children.length} ta farzand · bugun ${activeToday} tasi faol · ${pendingTotal} ta bajarilmagan vazifa`
    : "Farzandlaringizning faolligi, vazifalari va baholarini shu yerdan kuzatishingiz mumkin.";

  return (
    <DashboardShell role="PARENT">
      <div className="space-y-6">
        <DashboardHero
          title={firstName ? `Salom, ${firstName}!` : "Salom!"}
          description={heroDescription}
          actions={
            <>
              <Badge variant="role-parent">
                {children.length} farzand
              </Badge>
              <Badge variant={activeToday > 0 ? "success" : "outline"}>
                Bugun {activeToday} faol
              </Badge>
            </>
          }
        />

        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : null}

        {!dashboard && !error ? (
          <div className="grid gap-4 md:grid-cols-2">
            {Array.from({ length: 2 }).map((_, i) => (
              <Card key={i}>
                <CardContent className="space-y-3 pt-6">
                  <Skeleton className="h-5 w-1/2" />
                  <Skeleton className="h-4 w-1/3" />
                  <Skeleton className="h-4 w-2/3" />
                </CardContent>
              </Card>
            ))}
          </div>
        ) : null}

        {dashboard && children.length > 0 ? (
          <>
            <div className="stagger-rise grid grid-cols-3 gap-4">
              <StatTile label="Farzandlar" value={children.length} icon={Users} />
              <StatTile label="Bugun faol" value={activeToday} icon={Flame} />
              <StatTile label="Bajarilmagan vazifalar" value={pendingTotal} icon={BookOpen} />
            </div>

            <div className="stagger-rise grid gap-4 md:grid-cols-2">
              {children.map((child) => (
                <Link
                  key={child.studentId}
                  href={`/dashboard/parent/children/${child.studentId}`}
                  className="group block"
                >
                  <Card className="card-lift relative h-full overflow-hidden transition-colors group-hover:border-role-parent">
                    {/* Role-accent top hairline — same note as the shell's header bar */}
                    <span
                      aria-hidden
                      className="bg-role-parent absolute inset-x-0 top-0 h-0.5 opacity-50"
                    />
                    <CardContent className="flex flex-col gap-3 pt-6">
                      <div className="flex items-center gap-3">
                        <span className="bg-role-parent-muted text-role-parent flex size-10 shrink-0 items-center justify-center rounded-full font-heading text-sm font-semibold ring-1 ring-current">
                          {initials(child.name)}
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-semibold">{child.name}</p>
                          <p className="truncate text-sm text-muted-foreground">{child.className}</p>
                        </div>
                        {child.todayActivity ? (
                          <span
                            className="relative flex size-2.5 shrink-0"
                            title="Bugun faol bo'lgan"
                          >
                            <span className="bg-role-parent absolute inline-flex h-full w-full animate-ping rounded-full opacity-60" />
                            <span className="bg-role-parent relative inline-flex size-2.5 rounded-full" />
                          </span>
                        ) : (
                          <span
                            className="size-2.5 shrink-0 rounded-full bg-muted"
                            title="Bugun faollik yo'q"
                          />
                        )}
                        <ChevronRight
                          className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                          strokeWidth={1.75}
                        />
                      </div>

                      <div className="flex flex-wrap items-center gap-2">
                        <Badge variant={child.todayActivity ? "success" : "outline"}>
                          {child.todayActivity ? "Bugun faol bo'lgan" : "Bugun faollik yo'q"}
                        </Badge>
                        {child.pendingHomeworkCount > 0 ? (
                          <Badge variant="outline">
                            {child.pendingHomeworkCount} ta bajarilmagan vazifa
                          </Badge>
                        ) : (
                          <Badge variant="success">Barcha vazifalar bajarilgan</Badge>
                        )}
                      </div>

                      {child.recentGrade ? (
                        <p className="text-sm text-muted-foreground">
                          So&apos;nggi baho:{" "}
                          <span className="font-data font-medium text-foreground">
                            {child.recentGrade.score} ({child.recentGrade.fivePointGrade})
                          </span>
                        </p>
                      ) : (
                        <p className="text-sm text-muted-foreground">Hozircha baholar yo&apos;q.</p>
                      )}
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          </>
        ) : null}

        {dashboard && children.length === 0 ? (
          <div className="flex flex-col items-center gap-2 rounded-lg border-2 border-dashed border-border py-12 text-center">
            <Users className="size-8 text-muted-foreground" strokeWidth={1.75} />
            <p className="text-sm font-medium">Hozircha bog&apos;langan farzand yo&apos;q</p>
            <p className="text-sm text-muted-foreground">
              Maktab administratori bilan bog&apos;laning.
            </p>
          </div>
        ) : null}

        <BiometricConsentBanner />
      </div>
    </DashboardShell>
  );
}
