"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { Button } from "@/components/ui/button";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function AdminStudentsPage() {
  const students = useAdminStore((state) => state.students);
  const classes = useAdminStore((state) => state.classes);
  const fetchStudents = useAdminStore((state) => state.fetchStudents);
  const fetchClasses = useAdminStore((state) => state.fetchClasses);
  const createStudent = useAdminStore((state) => state.createStudent);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [classId, setClassId] = useState("");
  const [search, setSearch] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchClasses().catch(() => {});
    fetchStudents().catch(() => setError("O'quvchilar ro'yxatini yuklab bo'lmadi."));
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
    await fetchStudents({ search: search || undefined });
  }

  function classFullName(id: string | null) {
    return classes.find((c) => c.id === id)?.fullName ?? "—";
  }

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold">O&apos;quvchilar</h2>
        <Link href="/dashboard/admin/students/import" className="text-sm underline">
          Excel orqali ommaviy import
        </Link>
      </div>

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
        <div className="space-y-1">
          <label htmlFor="classId" className="text-sm font-medium">
            Sinf
          </label>
          <select
            id="classId"
            value={classId}
            onChange={(e) => setClassId(e.target.value)}
            required
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
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
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Qo'shilmoqda..." : "O'quvchi qo'shish"}
        </Button>
      </form>

      <form onSubmit={handleSearch} className="mb-4 flex items-end gap-3">
        <div className="space-y-1">
          <label htmlFor="search" className="text-sm font-medium">
            Qidirish
          </label>
          <input
            id="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Ism yoki familiya"
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          />
        </div>
        <Button type="submit" variant="outline">
          Qidirish
        </Button>
      </form>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">Ism familiya</th>
            <th className="py-2">Telefon</th>
            <th className="py-2">Sinf</th>
            <th className="py-2">Holati</th>
          </tr>
        </thead>
        <tbody>
          {students.map((student) => (
            <tr key={student.id} className="border-b border-border">
              <td className="py-2 font-medium">
                {student.firstName} {student.lastName}
              </td>
              <td className="py-2">{student.phone}</td>
              <td className="py-2">{classFullName(student.classId)}</td>
              <td className="py-2">{student.isActive ? "Faol" : "Faol emas"}</td>
            </tr>
          ))}
          {students.length === 0 ? (
            <tr>
              <td colSpan={4} className="py-4 text-center text-muted-foreground">
                Hozircha o&apos;quvchilar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>
    </DashboardShell>
  );
}
