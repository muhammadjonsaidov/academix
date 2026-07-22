"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, Users } from "lucide-react";
import { BiometricConsentBanner } from "@/components/parent/BiometricConsentBanner";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { PageHeader } from "@/components/shared/PageHeader";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useConsentStore } from "@/stores/useConsentStore";
import { useParentStore } from "@/stores/useParentStore";

export default function ParentDashboardPage() {
  const dashboard = useParentStore((state) => state.dashboard);
  const fetchDashboard = useParentStore((state) => state.fetchDashboard);
  const checkPendingConsent = useConsentStore((state) => state.checkPendingConsent);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Bosh sahifani yuklab bo'lmadi."));
  }, [fetchDashboard]);

  useEffect(() => {
    if (dashboard) {
      checkPendingConsent(dashboard.children);
    }
  }, [dashboard, checkPendingConsent]);

  return (
    <DashboardShell role="PARENT">
      <div className="space-y-6">
        <PageHeader
          title="Farzandlarim"
          description="Har bir farzandingiz bo'yicha faollik, vazifalar va baholarni shu yerdan kuzatishingiz mumkin."
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

        {dashboard && dashboard.children.length > 0 ? (
          <div className="stagger-rise grid gap-4 md:grid-cols-2">
            {dashboard.children.map((child) => (
              <Link
                key={child.studentId}
                href={`/dashboard/parent/children/${child.studentId}`}
                className="group block"
              >
                <Card className="card-lift h-full transition-colors group-hover:border-role-parent">
                  <CardContent className="flex flex-col gap-3 pt-6">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="font-semibold">{child.name}</p>
                        <p className="text-sm text-muted-foreground">{child.className}</p>
                      </div>
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
        ) : null}

        {dashboard && dashboard.children.length === 0 ? (
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
