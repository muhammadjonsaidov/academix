"use client";

import { useEffect, useState, type FormEvent } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { Button } from "@/components/ui/button";
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

  useEffect(() => {
    fetchTeachers().catch(() => setError("O'qituvchilar ro'yxatini yuklab bo'lmadi."));
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

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <h2 className="mb-4 text-lg font-semibold">O&apos;qituvchilar</h2>

      <form onSubmit={handleSubmit} className="mb-6 flex flex-wrap items-end gap-3">
        <div className="space-y-1">
          <label htmlFor="firstName" className="text-sm font-medium">
            Ism
          </label>
          <input
            id="firstName"
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="lastName" className="text-sm font-medium">
            Familiya
          </label>
          <input
            id="lastName"
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-1">
          <label htmlFor="phone" className="text-sm font-medium">
            Telefon raqam
          </label>
          <input
            id="phone"
            type="tel"
            placeholder="+998901234567"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Taklif qilinmoqda..." : "Taklif qilish"}
        </Button>
      </form>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Ism familiya</th>
            <th className="py-2">Telefon</th>
            <th className="py-2">Holati</th>
            <th className="py-2"></th>
          </tr>
        </thead>
        <tbody>
          {teachers.map((teacher) => (
            <tr key={teacher.id} className="border-b border-border">
              <td className="py-2 font-medium">
                {teacher.firstName} {teacher.lastName}
              </td>
              <td className="py-2">{teacher.phone}</td>
              <td className="py-2">{teacher.isActive ? "Faol" : "Faol emas"}</td>
              <td className="py-2 text-right">
                <Button
                  variant="outline"
                  onClick={() => setTeacherActive(teacher.id, !teacher.isActive)}
                >
                  {teacher.isActive ? "Faolsizlantirish" : "Faollashtirish"}
                </Button>
              </td>
            </tr>
          ))}
          {teachers.length === 0 ? (
            <tr>
              <td colSpan={4} className="py-4 text-center text-muted-foreground">
                Hozircha o&apos;qituvchilar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
