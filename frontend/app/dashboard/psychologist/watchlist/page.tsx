"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Eye, UserPlus } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/psychologist/EmptyState";
import { fieldClass, FormField } from "@/components/psychologist/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { usePsychologyStore } from "@/stores/usePsychologyStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function PsychologistWatchlistPage() {
  const watchlist = usePsychologyStore((state) => state.watchlist);
  const fetchWatchlist = usePsychologyStore((state) => state.fetchWatchlist);
  const addToWatchlist = usePsychologyStore((state) => state.addToWatchlist);
  const removeFromWatchlist = usePsychologyStore((state) => state.removeFromWatchlist);

  const [studentId, setStudentId] = useState("");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [removingId, setRemovingId] = useState<string | null>(null);

  useEffect(() => {
    fetchWatchlist()
      .catch(() => setError("Kuzatuv ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchWatchlist]);

  async function handleAdd(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await addToWatchlist(studentId, reason);
      setStudentId("");
      setReason("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Qo'shib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleRemove(id: string) {
    setError(null);
    setRemovingId(id);
    try {
      await removeFromWatchlist(id);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Olib tashlab bo'lmadi.");
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <DashboardShell role="PSYCHOLOGIST">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Kuzatuv ro&apos;yxati</h2>
          <p className="text-sm text-muted-foreground">
            Maxsus e&apos;tibor talab qiladigan o&apos;quvchilarni belgilang.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="size-4" strokeWidth={1.75} />
              Ro&apos;yxatga qo&apos;shish
            </CardTitle>
            <CardDescription>
              O&apos;quvchi ID va (ixtiyoriy) sababni kiriting.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleAdd} className="flex flex-wrap items-end gap-3">
              <FormField label="O'quvchi ID" htmlFor="studentId">
                <input
                  id="studentId"
                  value={studentId}
                  onChange={(e) => setStudentId(e.target.value)}
                  required
                  placeholder="uuid"
                  className={`${fieldClass} w-72`}
                />
              </FormField>
              <FormField label="Sabab" htmlFor="reason">
                <input
                  id="reason"
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className={`${fieldClass} w-72`}
                />
              </FormField>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Qo'shilmoqda..." : "Qo'shish"}
              </Button>
            </form>
            {error ? <p className="mt-3 text-sm text-destructive">{error}</p> : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Ro&apos;yxat</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : watchlist.length === 0 ? (
              <EmptyState
                icon={Eye}
                title="Kuzatuv ro'yxati bo'sh"
                description="Yuqoridagi shakl orqali birinchi o'quvchini kuzatuvga qo'shing."
              />
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">O&apos;quvchi</th>
                    <th className="py-2 font-medium">Sabab</th>
                    <th className="py-2 font-medium">Qo&apos;shilgan sana</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {watchlist.map((entry) => (
                    <tr key={entry.studentId} className="border-b border-border last:border-0">
                      <td className="py-2.5 font-medium">{entry.studentName}</td>
                      <td className="py-2.5 text-muted-foreground">{entry.reason ?? "—"}</td>
                      <td className="py-2.5 font-data text-muted-foreground">
                        {new Date(entry.addedAt).toLocaleDateString()}
                      </td>
                      <td className="py-2.5 text-right">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={removingId === entry.studentId}
                          onClick={() => handleRemove(entry.studentId)}
                        >
                          {removingId === entry.studentId ? "..." : "Olib tashlash"}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardShell>
  );
}
