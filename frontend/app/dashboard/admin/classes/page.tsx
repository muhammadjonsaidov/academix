"use client";

import { useEffect, useState, type FormEvent } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { Button } from "@/components/ui/button";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function AdminClassesPage() {
  const classes = useAdminStore((state) => state.classes);
  const fetchClasses = useAdminStore((state) => state.fetchClasses);
  const createClass = useAdminStore((state) => state.createClass);
  const deleteClass = useAdminStore((state) => state.deleteClass);

  const [grade, setGrade] = useState("");
  const [letter, setLetter] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchClasses().catch(() => setError("Sinflar ro'yxatini yuklab bo'lmadi."));
  }, [fetchClasses]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await createClass({ grade: Number(grade), letter: letter.toUpperCase() });
      setGrade("");
      setLetter("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Sinf yaratib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDelete(classId: string) {
    await deleteClass(classId);
  }

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <h2 className="mb-4 text-lg font-semibold">Sinflar</h2>

      <form onSubmit={handleSubmit} className="mb-6 flex items-end gap-3">
        <div className="space-y-1">
          <label htmlFor="grade" className="text-sm font-medium">
            Sinf raqami
          </label>
          <input
            id="grade"
            type="number"
            min={1}
            max={11}
            value={grade}
            onChange={(e) => setGrade(e.target.value)}
            required
            className="w-24 rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="letter" className="text-sm font-medium">
            Harfi
          </label>
          <input
            id="letter"
            type="text"
            maxLength={5}
            value={letter}
            onChange={(e) => setLetter(e.target.value)}
            required
            className="w-24 rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Qo'shilmoqda..." : "Sinf qo'shish"}
        </Button>
      </form>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Sinf</th>
            <th className="py-2">O&apos;quv yili</th>
            <th className="py-2">O&apos;quvchilar soni</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {classes.map((schoolClass) => (
            <tr key={schoolClass.id} className="border-b border-border">
              <td className="py-2 font-medium">{schoolClass.fullName}</td>
              <td className="py-2">{schoolClass.academicYear}</td>
              <td className="py-2">{schoolClass.studentCount}</td>
              <td className="py-2 text-right">
                <Button variant="outline" onClick={() => handleDelete(schoolClass.id)}>
                  O&apos;chirish
                </Button>
              </td>
            </tr>
          ))}
          {classes.length === 0 ? (
            <tr>
              <td colSpan={4} className="py-4 text-center text-muted-foreground">
                Hozircha sinflar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
