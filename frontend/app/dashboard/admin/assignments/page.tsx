"use client";

import { useEffect, useState, type FormEvent } from "react";
import { BookPlus, Link2, Trash2, X } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import { fieldClass, FormField, SelectField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import { SUBJECT_TYPES, type SubjectTypeName } from "@/types/admin";
import type { ApiErrorResponse } from "@/types/auth";

const SUBJECT_TYPE_LABELS: Record<SubjectTypeName, string> = {
  MATH: "Matematika",
  LANGUAGE_UZ: "O'zbek tili",
  LANGUAGE_RU: "Rus tili",
  LANGUAGE_EN: "Ingliz tili",
  PHYSICS: "Fizika",
  CHEMISTRY: "Kimyo",
  BIOLOGY: "Biologiya",
  HISTORY: "Tarix",
  GEOGRAPHY: "Geografiya",
  OTHER: "Boshqa",
};

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
  const createSubject = useAdminStore((state) => state.createSubject);
  const deleteSubject = useAdminStore((state) => state.deleteSubject);

  const [teacherId, setTeacherId] = useState("");
  const [classId, setClassId] = useState("");
  const [subjectId, setSubjectId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const [newSubjectName, setNewSubjectName] = useState("");
  const [newSubjectType, setNewSubjectType] = useState<SubjectTypeName>("OTHER");
  const [subjectError, setSubjectError] = useState<string | null>(null);
  const [isAddingSubject, setIsAddingSubject] = useState(false);
  const [removingSubjectId, setRemovingSubjectId] = useState<string | null>(null);

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

  async function handleAddSubject(event: FormEvent) {
    event.preventDefault();
    setSubjectError(null);
    setIsAddingSubject(true);
    try {
      await createSubject({ name: newSubjectName, type: newSubjectType });
      setNewSubjectName("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setSubjectError(apiError?.message ?? "Fanni qo'shib bo'lmadi.");
    } finally {
      setIsAddingSubject(false);
    }
  }

  async function handleRemoveSubject(id: string) {
    setSubjectError(null);
    setRemovingSubjectId(id);
    try {
      await deleteSubject(id);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setSubjectError(apiError?.message ?? "Fanni o'chirib bo'lmadi.");
    } finally {
      setRemovingSubjectId(null);
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
        <PageHeader
          title="Biriktirishlar"
          description="O'qituvchini sinf va fanga biriktiring."
          eyebrow="O'quv jarayoni"
        />

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BookPlus className="size-4" strokeWidth={1.75} />
              Fanlar
            </CardTitle>
            <CardDescription>
              Biriktirishdan oldin maktab fanlarini shu yerda yarating. Ishlatilayotgan fanni
              o&apos;chirib bo&apos;lmaydi.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <form onSubmit={handleAddSubject} className="flex flex-wrap items-end gap-3">
              <FormField label="Fan nomi" htmlFor="newSubjectName">
                <input
                  id="newSubjectName"
                  value={newSubjectName}
                  onChange={(e) => setNewSubjectName(e.target.value)}
                  placeholder="Masalan: Matematika"
                  required
                  className={`${fieldClass} min-w-52`}
                />
              </FormField>
              <FormField label="Turi" htmlFor="newSubjectType">
                <SelectField
                  id="newSubjectType"
                  value={newSubjectType}
                  onChange={(e) => setNewSubjectType(e.target.value as SubjectTypeName)}
                >
                  {SUBJECT_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {SUBJECT_TYPE_LABELS[type]}
                    </option>
                  ))}
                </SelectField>
              </FormField>
              <Button type="submit" disabled={isAddingSubject}>
                {isAddingSubject ? "Qo'shilmoqda..." : "Fan qo'shish"}
              </Button>
            </form>
            {subjects.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Hozircha fanlar yo&apos;q — birinchi fanni qo&apos;shing.
              </p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {subjects.map((subject) => (
                  <Badge key={subject.id} variant="secondary" className="gap-1.5">
                    {subject.name}
                    <button
                      type="button"
                      aria-label={`${subject.name} fanini o'chirish`}
                      disabled={removingSubjectId === subject.id}
                      onClick={() => handleRemoveSubject(subject.id)}
                      className="text-muted-foreground transition-colors hover:text-destructive"
                    >
                      <X className="size-3" strokeWidth={2} />
                    </button>
                  </Badge>
                ))}
              </div>
            )}
            {subjectError ? <p className="text-sm text-destructive">{subjectError}</p> : null}
          </CardContent>
        </Card>

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
            <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <FormField label="O'qituvchi" htmlFor="teacherId">
                <SelectField
                  id="teacherId"
                  value={teacherId}
                  onChange={(e) => setTeacherId(e.target.value)}
                  required
                >
                  <option value="" disabled>
                    Tanlang
                  </option>
                  {teachers.map((teacher) => (
                    <option key={teacher.id} value={teacher.id}>
                      {teacher.firstName} {teacher.lastName}
                    </option>
                  ))}
                </SelectField>
              </FormField>
              <FormField label="Sinf" htmlFor="classId">
                <SelectField
                  id="classId"
                  value={classId}
                  onChange={(e) => setClassId(e.target.value)}
                  required
                >
                  <option value="" disabled>
                    Tanlang
                  </option>
                  {classes.map((schoolClass) => (
                    <option key={schoolClass.id} value={schoolClass.id}>
                      {schoolClass.fullName}
                    </option>
                  ))}
                </SelectField>
              </FormField>
              <FormField label="Fan" htmlFor="subjectId">
                <SelectField
                  id="subjectId"
                  value={subjectId}
                  onChange={(e) => setSubjectId(e.target.value)}
                  required
                >
                  <option value="" disabled>
                    Tanlang
                  </option>
                  {subjects.map((subject) => (
                    <option key={subject.id} value={subject.id}>
                      {subject.name}
                    </option>
                  ))}
                </SelectField>
              </FormField>
              <div className="flex items-end sm:col-span-2 lg:col-span-3">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Qo'shilmoqda..." : "Biriktirish"}
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
