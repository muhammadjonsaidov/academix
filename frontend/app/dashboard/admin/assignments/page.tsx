"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Link2, Trash2 } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function AdminAssignmentsPage() {
  const assignments = useAdminStore((state) => state.assignments);
  const teachers = useAdminStore((state) => state.teachers);
  const classes = useAdminStore((state) => state.classes);
  const subjects = useAdminStore((state) => state.subjects);
  const fetchAssignments = useAdminStore((state) => state.fetchAssignments);
  const fetchTeachers = useAdminStore((state) => state.fetchTeachers);
  const fetchClasses = useAdminStore((state) => state.fetchClasses);
  const fetchSubjects = useAdminStore((state) => state.fetchSubjects);
  const createAssignment = useAdminStore((state) => state.createAssignment);
  const deleteAssignment = useAdminStore((state) => state.deleteAssignment);

  const [teacherId, setTeacherId] = useState("");
  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    fetchTeachers().catch(() => {});
    fetchClasses().catch(() => {});
    fetchSubjects().catch(() => {});
    fetchAssignments()
      .catch(() => setError("Biriktirishlar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchTeachers, fetchClasses, fetchSubjects, fetchAssignments]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await createAssignment({ teacherId, classId, subjectId });
      setTeacherId("");
      setClassId("");
      setSubjectId("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Biriktirish yaratib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDelete(assignmentId: string) {
    setDeletingId(assignmentId);
    try {
      await deleteAssignment(assignmentId);
    } finally {
      setDeletingId(null);
    }
  }

  function teacherName(id: string) {
    const teacher = teachers.find((t) => t.id === id);
    return teacher ? `${teacher.firstName} ${teacher.lastName}` : id;
  }

  function classFullName(id: string) {
    return classes.find((c) => c.id === id)?.fullName ?? id;
  }

  function subjectName(id: string) {
    return subjects.find((s) => s.id === id)?.name ?? id;
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Biriktirishlar</h2>
          <p className="text-sm text-muted-foreground">
            O&apos;qituvchini sinf va fanga biriktiring.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Link2 className="size-4" strokeWidth={1.75} />
              Yangi biriktirish qo&apos;shish
            </CardTitle>
            <CardDescription>
              O&apos;qituvchini sinf va fanga biriktiring.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
              <FormField label="O'qituvchi" htmlFor="teacherId">
                <select
                  id="teacherId"
                  value={teacherId}
                  onChange={(e) => setTeacherId(e.target.value)}
                  required
                  className={fieldClass}
                >
                  <option value="" disabled>
                    Tanlang
                  </option>
                  {teachers.map((teacher) => (
                    <option key={teacher.id} value={teacher.id}>
                      {teacher.firstName} {teacher.lastName}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField label="Sinf" htmlFor="classId">
                <select
                  id="classId"
                  value={classId}
                  onChange={(e) => setClassId(e.target.value)}
                  required
                  className={fieldClass}
                >
                  <option value="" disabled>
                    Tanlang
                  </option>
                  {classes.map((schoolClass) => (
                    <option key={schoolClass.id} value={schoolClass.id}>
                      {schoolClass.fullName}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField label="Fan" htmlFor="subjectId">
                <select
                  id="subjectId"
                  value={subjectId}
                  onChange={(e) => setSubjectId(e.target.value)}
                  required
                  className={fieldClass}
                >
                  <option value="" disabled>
                    Tanlang
                  </option>
                  {subjects.map((subject) => (
                    <option key={subject.id} value={subject.id}>
                      {subject.name}
                    </option>
                  ))}
                </select>
              </FormField>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Qo'shilmoqda..." : "Biriktirish"}
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
            ) : assignments.length === 0 ? (
              <EmptyState
                icon={Link2}
                title="Hozircha biriktirishlar yo'q"
                description="Yuqoridagi shakl orqali birinchi biriktirishni qo'shing."
              />
            ) : (
              <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">O&apos;qituvchi</th>
                    <th className="py-2 font-medium">Sinf</th>
                    <th className="py-2 font-medium">Fan</th>
                    <th className="py-2 font-medium">O&apos;quv yili</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {assignments.map((assignment) => (
                    <tr key={assignment.id} className="border-b border-border last:border-0">
                      <td className="py-2.5 font-medium">{teacherName(assignment.teacherId)}</td>
                      <td className="py-2.5">{classFullName(assignment.classId)}</td>
                      <td className="py-2.5 text-muted-foreground">
                        {subjectName(assignment.subjectId)}
                      </td>
                      <td className="py-2.5 font-data text-muted-foreground">
                        {assignment.academicYear}
                      </td>
                      <td className="py-2.5 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={deletingId === assignment.id}
                          onClick={() => handleDelete(assignment.id)}
                        >
                          <Trash2 className="size-3.5" strokeWidth={1.75} />
                          {deletingId === assignment.id ? "..." : "O'chirish"}
                        </Button>
                      </td>
                    </tr>
                  ))}
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
