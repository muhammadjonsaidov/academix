"use client";

import { useEffect, useState, type FormEvent } from "react";
import { UserPlus, Users } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { validatePassword } from "@/lib/password-policy";
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
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
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

    const policy = validatePassword(password);
    if (!policy.valid) {
      setError(policy.error);
      return;
    }

    setIsSubmitting(true);
    try {
      await inviteTeacher({
        phone,
        firstName,
        lastName,
        email: email || undefined,
        password,
      });
      setPhone("");
      setFirstName("");
      setLastName("");
      setEmail("");
      setPassword("");
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
        <PageHeader
          title="O'qituvchilar"
          description="Maktabga o'qituvchi taklif qiling va ularning holatini boshqaring."
          eyebrow="Jamoa"
        />

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="size-4" strokeWidth={1.75} />
              Yangi o&apos;qituvchi taklif qilish
            </CardTitle>
            <CardDescription>
              Telefon, email (ixtiyoriy) va boshlang&apos;ich parol bilan yaratiladi — parolni
              o&apos;qituvchiga o&apos;zingiz yetkazasiz, keyin o&apos;zi almashtirishi mumkin.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <FormField label="Ism" htmlFor="firstName">
                <input
                  id="firstName"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Familiya" htmlFor="lastName">
                <input
                  id="lastName"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
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
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Email (ixtiyoriy)" htmlFor="email">
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Boshlang'ich parol" htmlFor="password">
                <input
                  id="password"
                  type="text"
                  minLength={8}
                  placeholder="Kamida 8 belgi"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <div className="flex items-end sm:col-span-2 lg:col-span-3">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Taklif qilinmoqda..." : "Taklif qilish"}
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
