"use client";

import { useEffect, useState, type FormEvent } from "react";
import { School as SchoolIcon, Trash2 } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/admin/EmptyState";
import { fieldClass, FormField } from "@/components/admin/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
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
  const [isLoading, setIsLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    fetchClasses()
      .catch(() => setError("Sinflar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
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
    setDeletingId(classId);
    try {
      await deleteClass(classId);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Sinflar</h2>
          <p className="text-sm text-muted-foreground">Maktab sinflarini boshqaring.</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <SchoolIcon className="size-4" strokeWidth={1.75} />
              Yangi sinf qo&apos;shish
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="flex items-end gap-3">
              <FormField label="Sinf raqami" htmlFor="grade" className="w-24">
                <input
                  id="grade"
                  type="number"
                  min={1}
                  max={11}
                  value={grade}
                  onChange={(e) => setGrade(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Harfi" htmlFor="letter" className="w-24">
                <input
                  id="letter"
                  type="text"
                  maxLength={5}
                  value={letter}
                  onChange={(e) => setLetter(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Qo'shilmoqda..." : "Sinf qo'shish"}
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
            ) : classes.length === 0 ? (
              <EmptyState
                icon={SchoolIcon}
                title="Hozircha sinflar yo'q"
                description="Yuqoridagi shakl orqali birinchi sinfni qo'shing."
              />
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">Sinf</th>
                    <th className="py-2 font-medium">O&apos;quv yili</th>
                    <th className="py-2 font-medium">O&apos;quvchilar soni</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {classes.map((schoolClass) => (
                    <tr key={schoolClass.id} className="border-b border-border last:border-0">
                      <td className="py-2.5 font-medium">{schoolClass.fullName}</td>
                      <td className="py-2.5 font-data text-muted-foreground">
                        {schoolClass.academicYear}
                      </td>
                      <td className="py-2.5 font-data">{schoolClass.studentCount}</td>
                      <td className="py-2.5 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={deletingId === schoolClass.id}
                          onClick={() => handleDelete(schoolClass.id)}
                        >
                          <Trash2 className="size-3.5" strokeWidth={1.75} />
                          {deletingId === schoolClass.id ? "..." : "O'chirish"}
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
