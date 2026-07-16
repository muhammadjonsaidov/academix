"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BiometricConsentBanner } from "@/components/parent/BiometricConsentBanner";
import { ParentNav } from "@/components/parent/ParentNav";
import { DashboardShell } from "@/components/shared/DashboardShell";
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
      <ParentNav />
      <h2 className="mb-4 text-lg font-semibold">Farzandlarim</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {dashboard ? (
        <div className="grid gap-4 md:grid-cols-2">
          {dashboard.children.map((child) => (
            <Link
              key={child.studentId}
              href={`/dashboard/parent/children/${child.studentId}`}
              className="block rounded-md border border-border p-4 hover:border-foreground"
            >
              <p className="font-medium">
                {child.name} <span className="text-muted-foreground">({child.className})</span>
              </p>
              <p className="mt-2 text-sm text-muted-foreground">
                Bugungi faollik: {child.todayActivity ? "Ha" : "Yo'q"}
              </p>
              <p className="text-sm text-muted-foreground">
                Bajarilmagan vazifalar: {child.pendingHomeworkCount}
              </p>
              {child.recentGrade ? (
                <p className="text-sm text-muted-foreground">
                  So&apos;nggi baho: {child.recentGrade.score} ({child.recentGrade.fivePointGrade})
                </p>
              ) : null}
            </Link>
          ))}
          {dashboard.children.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Hozircha bog&apos;langan farzand yo&apos;q. Maktab administratori bilan bog&apos;laning.
            </p>
          ) : null}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Yuklanmoqda...</p>
      )}

      <BiometricConsentBanner />
    </DashboardShell>
  );
}
