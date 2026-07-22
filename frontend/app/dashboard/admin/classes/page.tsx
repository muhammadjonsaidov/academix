"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Pencil, School as SchoolIcon, Trash2, X } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function AdminClassesPage() {
  const classes = useAdminStore((state) => state.classes);
  const fetchClasses = useAdminStore((state) => state.fetchClasses);
  const createClass = useAdminStore((state) => state.createClass);
  const updateClass = useAdminStore((state) => state.updateClass);
  const deleteClass = useAdminStore((state) => state.deleteClass);

  const [grade, setGrade] = useState("");
  const [letter, setLetter] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editGrade, setEditGrade] = useState("");
  const [editLetter, setEditLetter] = useState("");
  const [editError, setEditError] = useState<string | null>(null);
  const [isUpdating, setIsUpdating] = useState(false);

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

  function startEdit(schoolClass: { id: string; grade: number; letter: string }) {
    setEditingId(schoolClass.id);
    setEditGrade(String(schoolClass.grade));
    setEditLetter(schoolClass.letter);
    setEditError(null);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditError(null);
  }

  async function handleUpdate(classId: string, classTeacherId: string | null) {
    setEditError(null);
    setIsUpdating(true);
    try {
      await updateClass(classId, {
        grade: Number(editGrade),
        letter: editLetter.toUpperCase(),
        classTeacherId: classTeacherId ?? undefined,
      });
      setEditingId(null);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setEditError(apiError?.message ?? "Sinfni yangilab bo'lmadi.");
    } finally {
      setIsUpdating(false);
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
            <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <FormField label="Sinf raqami" htmlFor="grade">
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
              <FormField label="Harfi" htmlFor="letter">
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
              <div className="flex items-end sm:col-span-2">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Qo'shilmoqda..." : "Sinf qo'shish"}
                </Button>
              </div>
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
              <div className="overflow-x-auto">
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
                  {classes.map((schoolClass) => {
                    const isEditing = editingId === schoolClass.id;
                    return (
                      <tr key={schoolClass.id} className="border-b border-border last:border-0">
                        {isEditing ? (
                          <>
                            <td className="py-2.5" colSpan={2}>
                              <div className="flex items-center gap-2">
                                <input
                                  type="number"
                                  min={1}
                                  max={11}
                                  value={editGrade}
                                  onChange={(e) => setEditGrade(e.target.value)}
                                  className={`${fieldClass} w-16`}
                                />
                                <input
                                  type="text"
                                  maxLength={5}
                                  value={editLetter}
                                  onChange={(e) => setEditLetter(e.target.value)}
                                  className={`${fieldClass} w-16`}
                                />
                                {editError ? (
                                  <span className="text-xs text-destructive">{editError}</span>
                                ) : null}
                              </div>
                            </td>
                            <td className="py-2.5 font-data">{schoolClass.studentCount}</td>
                            <td className="py-2.5 text-right">
                              <div className="flex justify-end gap-2">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  disabled={isUpdating}
                                  onClick={() => handleUpdate(schoolClass.id, schoolClass.classTeacherId)}
                                >
                                  {isUpdating ? "Saqlanmoqda..." : "Saqlash"}
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  disabled={isUpdating}
                                  onClick={cancelEdit}
                                >
                                  <X className="size-3.5" strokeWidth={1.75} />
                                </Button>
                              </div>
                            </td>
                          </>
                        ) : (
                          <>
                            <td className="py-2.5 font-medium">{schoolClass.fullName}</td>
                            <td className="py-2.5 font-data text-muted-foreground">
                              {schoolClass.academicYear}
                            </td>
                            <td className="py-2.5 font-data">{schoolClass.studentCount}</td>
                            <td className="py-2.5 text-right">
                              <div className="flex justify-end gap-2">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => startEdit(schoolClass)}
                                >
                                  <Pencil className="size-3.5" strokeWidth={1.75} />
                                  Tahrirlash
                                </Button>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  disabled={deletingId === schoolClass.id}
                                  onClick={() => handleDelete(schoolClass.id)}
                                >
                                  <Trash2 className="size-3.5" strokeWidth={1.75} />
                                  {deletingId === schoolClass.id ? "..." : "O'chirish"}
                                </Button>
                              </div>
                            </td>
                          </>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardShell>
  );
}
