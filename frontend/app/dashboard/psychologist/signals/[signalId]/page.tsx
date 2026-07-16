"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { PsychologistNav } from "@/components/psychologist/PsychologistNav";
import { Button } from "@/components/ui/button";
import { usePsychologyStore } from "@/stores/usePsychologyStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function PsychologistSignalDetailPage() {
  const params = useParams<{ signalId: string }>();
  const signalId = params.signalId;
  const detail = usePsychologyStore((state) => state.selectedSignal);
  const fetchSignalDetail = usePsychologyStore((state) => state.fetchSignalDetail);
  const resolveSignal = usePsychologyStore((state) => state.resolveSignal);
  const markManipulation = usePsychologyStore((state) => state.markManipulation);

  const [notes, setNotes] = useState("");
  const [actionTaken, setActionTaken] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isResolving, setIsResolving] = useState(false);
  const [isMarking, setIsMarking] = useState(false);

  useEffect(() => {
    fetchSignalDetail(signalId).catch(() => setError("Signalni yuklab bo'lmadi."));
  }, [fetchSignalDetail, signalId]);

  async function handleResolve(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsResolving(true);
    try {
      await resolveSignal(signalId, { notes, actionTaken });
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Hal qilib bo'lmadi.");
    } finally {
      setIsResolving(false);
    }
  }

  async function handleMarkManipulation() {
    setError(null);
    setIsMarking(true);
    try {
      await markManipulation(signalId);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Belgilab bo'lmadi.");
    } finally {
      setIsMarking(false);
    }
  }

  if (!detail) {
    return (
      <DashboardShell role="PSYCHOLOGIST">
        <PsychologistNav />
        {error ? <p className="text-sm text-destructive">{error}</p> : <p>Yuklanmoqda...</p>}
      </DashboardShell>
    );
  }

  const { signal, studentBehaviorProfile } = detail;

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <PsychologistNav />
      <h2 className="mb-1 text-lg font-semibold">
        {signal.studentName} — {signal.className}
      </h2>
      <p className="mb-4 text-sm text-muted-foreground">
        {signal.type} · {signal.severity} · {new Date(signal.detectedAt).toLocaleString()}
        {signal.isManipulation ? (
          <span className="ml-2 text-destructive">⚠ Manipulyatsiya shubhasi</span>
        ) : null}
      </p>

      {signal.description ? (
        <p className="mb-6 rounded-md border border-border p-4 text-sm">{signal.description}</p>
      ) : (
        <p className="mb-6 text-sm text-muted-foreground">
          Tavsif mavjud emas (anonimlashtirilgan yoki hali aniqlanmagan).
        </p>
      )}

      <div className="mb-6 space-y-3 rounded-md border border-border p-4">
        <p className="font-medium">Xulq-atvor profili</p>
        <p className="text-xs text-muted-foreground">
          Faqat faollik ma&apos;lumotlari — dars mazmuni yoki baholar ko&apos;rinmaydi.
        </p>

        <div>
          <p className="mb-1 text-sm font-medium">Faol soatlar</p>
          <div className="flex flex-wrap gap-2 text-xs">
            {Object.entries(studentBehaviorProfile.activeHours).map(([range, count]) => (
              <span key={range} className="rounded-md border border-border px-2 py-1">
                {range}: {count}
              </span>
            ))}
          </div>
        </div>

        <div>
          <p className="mb-1 text-sm font-medium">Topshirish naqshi (oxirgi 14 kun)</p>
          <div className="flex flex-wrap gap-2 text-xs">
            {studentBehaviorProfile.submissionPattern.map((day) => (
              <span key={day.date} className="rounded-md border border-border px-2 py-1">
                {day.date}: {day.count}
              </span>
            ))}
            {studentBehaviorProfile.submissionPattern.length === 0 ? (
              <span className="text-muted-foreground">Topshiriq yo&apos;q.</span>
            ) : null}
          </div>
        </div>

        <div>
          <p className="mb-1 text-sm font-medium">XP tendensiyasi</p>
          <ul className="space-y-1 text-xs">
            {studentBehaviorProfile.xpTrend.slice(0, 5).map((entry) => (
              <li key={entry.id}>
                {new Date(entry.occurredAt).toLocaleDateString()}: +{entry.xp} ({entry.reason})
              </li>
            ))}
            {studentBehaviorProfile.xpTrend.length === 0 ? (
              <li className="text-muted-foreground">XP tarixi yo&apos;q.</li>
            ) : null}
          </ul>
        </div>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {signal.resolved ? (
        <div className="rounded-md border border-border p-4">
          <p className="font-medium">Hal qilingan</p>
          {signal.resolutionNotes ? <p className="mt-2 text-sm">{signal.resolutionNotes}</p> : null}
          {signal.actionTaken ? (
            <p className="mt-1 text-sm text-muted-foreground">Chora: {signal.actionTaken}</p>
          ) : null}
        </div>
      ) : (
        <form onSubmit={handleResolve} className="space-y-3 rounded-md border border-border p-4">
          <p className="font-medium">Hal qilish</p>
          <div className="space-y-1">
            <label htmlFor="notes" className="text-sm font-medium">
              Izoh
            </label>
            <textarea
              id="notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              required
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            />
          </div>
          <div className="space-y-1">
            <label htmlFor="actionTaken" className="text-sm font-medium">
              Ko&apos;rilgan chora
            </label>
            <input
              id="actionTaken"
              value={actionTaken}
              onChange={(e) => setActionTaken(e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            />
          </div>
          <div className="flex gap-3">
            <Button type="submit" disabled={isResolving}>
              {isResolving ? "Saqlanmoqda..." : "Hal qilish"}
            </Button>
            {!signal.isManipulation ? (
              <Button
                type="button"
                variant="outline"
                disabled={isMarking}
                onClick={handleMarkManipulation}
              >
                {isMarking ? "..." : "Manipulyatsiya deb belgilash"}
              </Button>
            ) : null}
          </div>
        </form>
      )}
    </DashboardShell>
  );
}
