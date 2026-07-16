"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { PsychologistNav } from "@/components/psychologist/PsychologistNav";
import { usePsychologyStore } from "@/stores/usePsychologyStore";

export default function PsychologistDashboardPage() {
  const dashboard = usePsychologyStore((state) => state.dashboard);
  const fetchDashboard = usePsychologyStore((state) => state.fetchDashboard);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard().catch(() => setError("Bosh sahifani yuklab bo'lmadi."));
  }, [fetchDashboard]);

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <PsychologistNav />
      <h2 className="mb-4 text-lg font-semibold">Bosh sahifa</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {dashboard ? (
        <>
          <div className="mb-6 grid grid-cols-4 gap-4">
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold text-destructive">{dashboard.criticalSignals}</p>
              <p className="text-sm text-muted-foreground">Kritik</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold text-amber-600">{dashboard.highSignals}</p>
              <p className="text-sm text-muted-foreground">Yuqori</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold">{dashboard.mediumSignals}</p>
              <p className="text-sm text-muted-foreground">O&apos;rta</p>
            </div>
            <div className="rounded-md border border-border p-4">
              <p className="text-2xl font-semibold text-green-600">{dashboard.resolvedThisWeek}</p>
              <p className="text-sm text-muted-foreground">Shu hafta hal qilindi</p>
            </div>
          </div>

          <h3 className="mb-2 text-sm font-semibold">Kuzatuv ro&apos;yxati</h3>
          <ul className="space-y-2">
            {dashboard.watchlistStudents.map((s) => (
              <li key={s.studentId} className="rounded-md border border-border p-3 text-sm">
                <span className="font-medium">{s.studentName}</span>
                {s.reason ? <span className="text-muted-foreground"> — {s.reason}</span> : null}
              </li>
            ))}
            {dashboard.watchlistStudents.length === 0 ? (
              <p className="text-sm text-muted-foreground">Kuzatuv ro&apos;yxati bo&apos;sh.</p>
            ) : null}
          </ul>

          <Link href="/dashboard/psychologist/signals" className="mt-6 inline-block text-sm underline">
            Barcha signallarni ko&apos;rish
          </Link>
        </>
      ) : (
        <p className="text-sm text-muted-foreground">Yuklanmoqda...</p>
      )}
    </DashboardShell>
  );
}
