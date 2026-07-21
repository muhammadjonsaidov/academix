"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { AlertTriangle, CheckCircle2, HeartPulse } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass, FormField } from "@/components/shared/FormField";
import {
  CriticalNotifyNote,
  SEVERITY_BADGE_VARIANT,
  SEVERITY_LABEL,
  TYPE_LABEL,
} from "@/components/psychologist/severity";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
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
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchSignalDetail(signalId)
      .catch(() => setError("Signalni yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
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

  if (isLoading) {
    return (
      <DashboardShell role="PSYCHOLOGIST">
        <div className="space-y-6">
          <Skeleton className="h-7 w-64" />
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-56 w-full" />
        </div>
      </DashboardShell>
    );
  }

  if (!detail) {
    return (
      <DashboardShell role="PSYCHOLOGIST">
        {error ? (
          <p className="text-sm text-destructive">{error}</p>
        ) : (
          <p className="text-sm text-muted-foreground">Signal topilmadi.</p>
        )}
      </DashboardShell>
    );
  }

  const { signal, studentBehaviorProfile } = detail;
  const isCritical = signal.severity === "CRITICAL";

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">
            {signal.studentName} — {signal.className}
          </h2>
          <div className="mt-1.5 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
            <span>{TYPE_LABEL[signal.type] ?? signal.type}</span>
            <Badge variant={SEVERITY_BADGE_VARIANT[signal.severity]}>
              {SEVERITY_LABEL[signal.severity]}
            </Badge>
            <span className="font-data">{new Date(signal.detectedAt).toLocaleString()}</span>
            {signal.isManipulation ? (
              <Badge variant="destructive" className="gap-1">
                <AlertTriangle className="size-3" strokeWidth={1.75} />
                Manipulyatsiya shubhasi
              </Badge>
            ) : null}
          </div>
        </div>

        <Card className={isCritical ? "rail-critical" : undefined}>
          <CardContent className="space-y-2 pt-6">
            {signal.description ? (
              <p className="text-sm">{signal.description}</p>
            ) : (
              <p className="text-sm text-muted-foreground">
                Tavsif mavjud emas (anonimlashtirilgan yoki hali aniqlanmagan).
              </p>
            )}
            {isCritical ? <CriticalNotifyNote /> : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <HeartPulse className="size-4" strokeWidth={1.75} />
              Xulq-atvor profili
            </CardTitle>
            <CardDescription>
              Faqat faollik ma&apos;lumotlari — dars mazmuni yoki baholar ko&apos;rinmaydi.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <p className="mb-1.5 text-sm font-medium">Faol soatlar</p>
              <div className="flex flex-wrap gap-2 text-xs">
                {Object.entries(studentBehaviorProfile.activeHours).map(([range, count]) => (
                  <span
                    key={range}
                    className="rounded-md border border-border px-2 py-1 font-data"
                  >
                    {range}: {count}
                  </span>
                ))}
                {Object.keys(studentBehaviorProfile.activeHours).length === 0 ? (
                  <span className="text-muted-foreground">Ma&apos;lumot yo&apos;q.</span>
                ) : null}
              </div>
            </div>

            <div>
              <p className="mb-1.5 text-sm font-medium">Topshirish naqshi (oxirgi 14 kun)</p>
              <div className="flex flex-wrap gap-2 text-xs">
                {studentBehaviorProfile.submissionPattern.map((day) => (
                  <span
                    key={day.date}
                    className="rounded-md border border-border px-2 py-1 font-data"
                  >
                    {day.date}: {day.count}
                  </span>
                ))}
                {studentBehaviorProfile.submissionPattern.length === 0 ? (
                  <span className="text-muted-foreground">Topshiriq yo&apos;q.</span>
                ) : null}
              </div>
            </div>

            <div>
              <p className="mb-1.5 text-sm font-medium">XP tendensiyasi</p>
              <ul className="space-y-1 text-xs">
                {studentBehaviorProfile.xpTrend.slice(0, 5).map((entry) => (
                  <li key={entry.id} className="font-data">
                    {new Date(entry.occurredAt).toLocaleDateString()}: +{entry.xp} ({entry.reason})
                  </li>
                ))}
                {studentBehaviorProfile.xpTrend.length === 0 ? (
                  <li className="font-sans text-muted-foreground">XP tarixi yo&apos;q.</li>
                ) : null}
              </ul>
            </div>
          </CardContent>
        </Card>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        {signal.resolved ? (
          <Card>
            <CardContent className="flex items-start gap-3 pt-6">
              <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-success-bg text-success">
                <CheckCircle2 className="size-4" strokeWidth={1.75} />
              </span>
              <div>
                <p className="font-medium">Hal qilingan</p>
                {signal.resolutionNotes ? (
                  <p className="mt-1 text-sm text-muted-foreground">{signal.resolutionNotes}</p>
                ) : null}
                {signal.actionTaken ? (
                  <p className="mt-1 text-sm text-muted-foreground">
                    Chora: {signal.actionTaken}
                  </p>
                ) : null}
              </div>
            </CardContent>
          </Card>
        ) : (
          <Card>
            <CardHeader>
              <CardTitle>Hal qilish</CardTitle>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleResolve} className="space-y-3">
                <FormField label="Izoh" htmlFor="notes">
                  <textarea
                    id="notes"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    required
                    rows={3}
                    className={`${fieldClass} h-auto w-full py-2`}
                  />
                </FormField>
                <FormField label="Ko'rilgan chora" htmlFor="actionTaken">
                  <input
                    id="actionTaken"
                    value={actionTaken}
                    onChange={(e) => setActionTaken(e.target.value)}
                    className={`${fieldClass} w-full`}
                  />
                </FormField>
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
            </CardContent>
          </Card>
        )}
      </div>
    </DashboardShell>
  );
}
