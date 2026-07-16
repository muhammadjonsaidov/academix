"use client";

import { useEffect, useState, type FormEvent } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { PsychologistNav } from "@/components/psychologist/PsychologistNav";
import { Button } from "@/components/ui/button";
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
  const [removingId, setRemovingId] = useState<string | null>(null);

  useEffect(() => {
    fetchWatchlist().catch(() => setError("Kuzatuv ro'yxatini yuklab bo'lmadi."));
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
      <PsychologistNav />
      <h2 className="mb-4 text-lg font-semibold">Kuzatuv ro&apos;yxati</h2>

      <form onSubmit={handleAdd} className="mb-6 flex flex-wrap items-end gap-3">
        <div className="space-y-1">
          <label htmlFor="studentId" className="text-sm font-medium">
            O&apos;quvchi ID
          </label>
          <input
            id="studentId"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value)}
            required
            placeholder="uuid"
            className="w-72 rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="reason" className="text-sm font-medium">
            Sabab
          </label>
          <input
            id="reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-72 rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Qo'shilmoqda..." : "Qo'shish"}
        </Button>
      </form>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">O&apos;quvchi</th>
            <th className="py-2">Sabab</th>
            <th className="py-2">Qo&apos;shilgan sana</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {watchlist.map((entry) => (
            <tr key={entry.studentId} className="border-b border-border">
              <td className="py-2 font-medium">{entry.studentName}</td>
              <td className="py-2">{entry.reason ?? "—"}</td>
              <td className="py-2">{new Date(entry.addedAt).toLocaleDateString()}</td>
              <td className="py-2 text-right">
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
          {watchlist.length === 0 ? (
            <tr>
              <td colSpan={4} className="py-4 text-center text-muted-foreground">
                Kuzatuv ro&apos;yxati bo&apos;sh.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
