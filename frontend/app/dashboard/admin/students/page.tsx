"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { FileSpreadsheet, GraduationCap, Search, UserPlus } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function AdminStudentsPage() {
  const students = useAdminStore((state) => state.students);
  const classes = useAdminStore((state) => state.classes);
  const fetchStudents = useAdminStore((state) => state.fetchStudents);
  const fetchClasses = useAdminStore((state) => state.fetchClasses);
  const createStudent = useAdminStore((state) => state.createStudent);
  const unlockHandwritingReset = useAdminStore((state) => state.unlockHandwritingReset);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [classId, setClassId] = useState("");
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [unlockingId, setUnlockingId] = useState<string | null>(null);
  const [unlockMessage, setUnlockMessage] = useState<{
    studentId: string;
    text: string;
    isError: boolean;
  } | null>(null);

  useEffect(() => {
    fetchClasses().catch(() => {});
    fetchStudents()
      .catch(() => setError("O'quvchilar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchClasses, fetchStudents]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await createStudent({ firstName, lastName, phone, classId });
      setFirstName("");
      setLastName("");
      setPhone("");
      setClassId("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "O'quvchi qo'shib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSearch(event: FormEvent) {
    event.preventDefault();
    setIsLoading(true);
    await fetchStudents({ search: search || undefined }).finally(() => setIsLoading(false));
  }

  function classFullName(id: string | null) {
    return classes.find((c) => c.id === id)?.fullName ?? "—";
  }

  async function handleUnlockReset(studentId: string) {
    setUnlockingId(studentId);
    setUnlockMessage(null);
    try {
      await unlockHandwritingReset(studentId);
      setUnlockMessage({ studentId, text: "Reset limiti tiklandi.", isError: false });
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setUnlockMessage({
        studentId,
        text: apiError?.message ?? "Reset limitini tiklab bo'lmadi.",
        isError: true,
      });
    } finally {
      setUnlockingId(null);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-heading text-xl font-semibold">O&apos;quvchilar</h2>
            <p className="text-sm text-muted-foreground">
              O&apos;quvchilarni qo&apos;shing yoki Excel orqali ommaviy import qiling.
            </p>
          </div>
          <Button variant="outline" render={<Link href="/dashboard/admin/students/import" />}>
            <FileSpreadsheet className="size-4" strokeWidth={1.75} />
            Excel orqali ommaviy import
          </Button>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="size-4" strokeWidth={1.75} />
              Yangi o&apos;quvchi qo&apos;shish
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-3">
              <FormField label="Ism" htmlFor="firstName">
                <input
                  id="firstName"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                  className={fieldClass}
                />
              </FormField>
              <FormField label="Familiya" htmlFor="lastName">
                <input
                  id="lastName"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                  className={fieldClass}
                />
              </FormField>
              <FormField label="Telefon raqam" htmlFor="phone">
                <input
                  id="phone"
                  type="tel"
                  placeholder="+998901234567"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                  className={fieldClass}
                />
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
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Qo'shilmoqda..." : "O'quvchi qo'shish"}
              </Button>
            </form>
            {error ? <p className="mt-3 text-sm text-destructive">{error}</p> : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Ro&apos;yxat</CardTitle>
            <form onSubmit={handleSearch} className="mt-2 flex items-end gap-2">
              <FormField label="Qidirish" htmlFor="search" className="w-64">
                <input
                  id="search"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Ism yoki familiya"
                  className={fieldClass}
                />
              </FormField>
              <Button type="submit" variant="outline" size="sm">
                <Search className="size-3.5" strokeWidth={1.75} />
                Qidirish
              </Button>
            </form>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : students.length === 0 ? (
              <EmptyState
                icon={GraduationCap}
                title="Hozircha o'quvchilar yo'q"
                description="Yuqoridagi shakl orqali qo'shing yoki Excel orqali ommaviy import qiling."
              />
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">Ism familiya</th>
                    <th className="py-2 font-medium">Telefon</th>
                    <th className="py-2 font-medium">Sinf</th>
                    <th className="py-2 font-medium">Holati</th>
                    <th className="py-2 font-medium">Yozuv profili</th>
                  </tr>
                </thead>
                <tbody>
                  {students.map((student) => (
                    <tr key={student.id} className="border-b border-border last:border-0">
                      <td className="py-2.5 font-medium">
                        {student.firstName} {student.lastName}
                      </td>
                      <td className="py-2.5 font-data text-muted-foreground">{student.phone}</td>
                      <td className="py-2.5">{classFullName(student.classId)}</td>
                      <td className="py-2.5">
                        <Badge variant={student.isActive ? "success" : "secondary"}>
                          {student.isActive ? "Faol" : "Faol emas"}
                        </Badge>
                      </td>
                      <td className="py-2.5">
                        <div className="flex flex-col items-start gap-1">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            disabled={unlockingId === student.id}
                            onClick={() => handleUnlockReset(student.id)}
                          >
                            {unlockingId === student.id ? "Tiklanmoqda..." : "Reset limitini tiklash"}
                          </Button>
                          {unlockMessage?.studentId === student.id ? (
                            <span
                              className={`text-xs ${unlockMessage.isError ? "text-destructive" : "text-success"}`}
                            >
                              {unlockMessage.text}
                            </span>
                          ) : null}
                        </div>
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
