"use client";

import { useEffect, useState, type FormEvent } from "react";
import { UserPlus, Users } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function AdminTeachersPage() {
  const teachers = useAdminStore((state) => state.teachers);
  const fetchTeachers = useAdminStore((state) => state.fetchTeachers);
  const inviteTeacher = useAdminStore((state) => state.inviteTeacher);
  const setTeacherActive = useAdminStore((state) => state.setTeacherActive);

  const [phone, setPhone] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  useEffect(() => {
    fetchTeachers()
      .catch(() => setError("O'qituvchilar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchTeachers]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await inviteTeacher({ phone, firstName, lastName });
      setPhone("");
      setFirstName("");
      setLastName("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "O'qituvchi taklif qilib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleToggleActive(teacherId: string, active: boolean) {
    setTogglingId(teacherId);
    try {
      await setTeacherActive(teacherId, active);
    } finally {
      setTogglingId(null);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">O&apos;qituvchilar</h2>
          <p className="text-sm text-muted-foreground">
            Maktabga o&apos;qituvchi taklif qiling va ularning holatini boshqaring.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="size-4" strokeWidth={1.75} />
              Yangi o&apos;qituvchi taklif qilish
            </CardTitle>
            <CardDescription>
              Taklif SMS orqali yuboriladi, o&apos;qituvchi birinchi kirishda parolini o&apos;rnatadi.
            </CardDescription>
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
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Taklif qilinmoqda..." : "Taklif qilish"}
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
            ) : teachers.length === 0 ? (
              <EmptyState
                icon={Users}
                title="Hozircha o'qituvchilar yo'q"
                description="Yuqoridagi shakl orqali birinchi o'qituvchini taklif qiling."
              />
            ) : (
              <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">Ism familiya</th>
                    <th className="py-2 font-medium">Telefon</th>
                    <th className="py-2 font-medium">Holati</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {teachers.map((teacher) => (
                    <tr key={teacher.id} className="border-b border-border last:border-0">
                      <td className="py-2.5 font-medium">
                        {teacher.firstName} {teacher.lastName}
                      </td>
                      <td className="py-2.5 font-data text-muted-foreground">{teacher.phone}</td>
                      <td className="py-2.5">
                        <Badge variant={teacher.isActive ? "success" : "secondary"}>
                          {teacher.isActive ? "Faol" : "Faol emas"}
                        </Badge>
                      </td>
                      <td className="py-2.5 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={togglingId === teacher.id}
                          onClick={() => handleToggleActive(teacher.id, !teacher.isActive)}
                        >
                          {togglingId === teacher.id
                            ? "..."
                            : teacher.isActive
                              ? "Faolsizlantirish"
                              : "Faollashtirish"}
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
