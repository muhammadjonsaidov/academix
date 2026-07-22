"use client";

import { useEffect, useState } from "react";
import { ListChecks, Plus, Trash2 } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { CriteriaItem } from "@/types/teacher";

const DEFAULT_ROW: CriteriaItem = { name: "", weightPercent: 0, description: "" };

export default function TeacherGradingCriteriaPage() {
  const subjects = useTeacherStore((state) => state.subjects);
  const fetchSubjects = useTeacherStore((state) => state.fetchSubjects);
  const fetchGradingCriteria = useTeacherStore((state) => state.fetchGradingCriteria);
  const updateGradingCriteria = useTeacherStore((state) => state.updateGradingCriteria);

  const [subjectId, setSubjectId] = useState("");
  const [rows, setRows] = useState<CriteriaItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    fetchSubjects().catch(() => {});
  }, [fetchSubjects]);

  useEffect(() => {
    let ignore = false;

    async function load() {
      if (!subjectId) {
        if (!ignore) setRows([{ ...DEFAULT_ROW }]);
        return;
      }
      try {
        await fetchGradingCriteria(subjectId);
        if (ignore) return;
        const current = useTeacherStore.getState().gradingCriteria;
        setRows(current.length > 0 ? current : [{ ...DEFAULT_ROW }]);
        setError(null);
        setMessage(null);
      } catch {
        if (!ignore) setError("Mezonlarni yuklab bo'lmadi.");
      }
    }

    load();
    return () => {
      ignore = true;
    };
  }, [subjectId, fetchGradingCriteria]);

  const weightSum = rows.reduce((sum, row) => sum + Number(row.weightPercent || 0), 0);

  function updateRow(index: number, patch: Partial<CriteriaItem>) {
    setRows((prev) => prev.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  }

  function addRow() {
    setRows((prev) => [...prev, { ...DEFAULT_ROW }]);
  }

  function removeRow(index: number) {
    setRows((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSave() {
    setError(null);
    setMessage(null);
    setIsSaving(true);
    try {
      await updateGradingCriteria(subjectId, rows);
      setMessage("Saqlandi.");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Saqlab bo'lmadi.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <DashboardShell role="TEACHER">
      <h2 className="mb-4 font-heading text-lg font-semibold">Baholash mezonlari</h2>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <ListChecks className="size-4 text-muted-foreground" strokeWidth={1.75} />
            Fan bo&apos;yicha mezonlar
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1">
            <label htmlFor="subjectId" className="text-sm font-medium">
              Fan
            </label>
            <select
              id="subjectId"
              value={subjectId}
              onChange={(e) => setSubjectId(e.target.value)}
              className="block rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="" disabled>
                Tanlang
              </option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          {subjectId ? (
            <div className="space-y-3">
              <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr>
                    <th className="py-2">Mezon nomi</th>
                    <th className="py-2">Og&apos;irlik (%)</th>
                    <th className="py-2">Tavsif</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row, i) => (
                    <tr key={i} className="border-b border-border last:border-0">
                      <td className="py-2 pr-2">
                        <input
                          value={row.name}
                          onChange={(e) => updateRow(i, { name: e.target.value })}
                          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        />
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          type="number"
                          min={0}
                          max={100}
                          value={row.weightPercent}
                          onChange={(e) => updateRow(i, { weightPercent: Number(e.target.value) })}
                          className="w-24 rounded-md border border-input bg-background px-3 py-2 text-sm font-data"
                        />
                      </td>
                      <td className="py-2 pr-2">
                        <input
                          value={row.description}
                          onChange={(e) => updateRow(i, { description: e.target.value })}
                          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                        />
                      </td>
                      <td className="py-2">
                        <Button variant="outline" size="icon" aria-label="O'chirish" onClick={() => removeRow(i)}>
                          <Trash2 className="size-4" strokeWidth={1.75} />
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <Button variant="outline" onClick={addRow}>
                  <Plus className="size-4" strokeWidth={1.75} />
                  Mezon qo&apos;shish
                </Button>
                <span
                  className={`font-data text-sm ${weightSum === 100 ? "text-muted-foreground" : "text-destructive"}`}
                >
                  Jami: {weightSum}%
                </span>
              </div>

              {error ? <p className="text-sm text-destructive">{error}</p> : null}
              {message ? <p className="text-sm text-success">{message}</p> : null}

              <Button onClick={handleSave} disabled={isSaving}>
                {isSaving ? "Saqlanmoqda..." : "Saqlash"}
              </Button>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">Mezonlarni ko&apos;rish uchun fan tanlang.</p>
          )}
        </CardContent>
      </Card>
    </DashboardShell>
  );
}
