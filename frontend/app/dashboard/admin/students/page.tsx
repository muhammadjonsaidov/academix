"use client";

import { Fragment, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import {
  FileSpreadsheet,
  GraduationCap,
  Link2,
  Search,
  UserPlus,
  Users,
} from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField, SelectField } from "@/components/shared/FormField";
import { PaginationControl } from "@/components/shared/PaginationControl";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { ParentRelation } from "@/types/admin";

const RELATION_LABELS: Record<ParentRelation, string> = {
  MOTHER: "Ona",
  FATHER: "Ota",
  GUARDIAN: "Vasiy",
};

export default function AdminStudentsPage() {
  const students = useAdminStore((state) => state.students);
  const classes = useAdminStore((state) => state.classes);
  const fetchStudents = useAdminStore((state) => state.fetchStudents);
  const studentsPage = useAdminStore((state) => state.studentsPage);
  const studentsPageSize = useAdminStore((state) => state.studentsPageSize);
  const studentsTotalItems = useAdminStore((state) => state.studentsTotalItems);
  const setStudentsPage = useAdminStore((state) => state.setStudentsPage);
  const setStudentsPageSize = useAdminStore((state) => state.setStudentsPageSize);
  const fetchClasses = useAdminStore((state) => state.fetchClasses);
  const createStudent = useAdminStore((state) => state.createStudent);
  const unlockHandwritingReset = useAdminStore((state) => state.unlockHandwritingReset);
  const transferStudentClass = useAdminStore((state) => state.transferStudentClass);
  const linkParentToStudent = useAdminStore((state) => state.linkParentToStudent);

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

  const [openPanel, setOpenPanel] = useState<{
    studentId: string;
    type: "transfer" | "link";
  } | null>(null);

  const [transferClassId, setTransferClassId] = useState("");
  const [isTransferring, setIsTransferring] = useState(false);
  const [transferMessage, setTransferMessage] = useState<{
    studentId: string;
    text: string;
    isError: boolean;
  } | null>(null);

  const [linkPhone, setLinkPhone] = useState("");
  const [linkRelation, setLinkRelation] = useState<ParentRelation>("MOTHER");
  const [isLinking, setIsLinking] = useState(false);
  const [linkMessage, setLinkMessage] = useState<{
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

  function openTransferPanel(student: { id: string; classId: string | null }) {
    setOpenPanel({ studentId: student.id, type: "transfer" });
    setTransferClassId(student.classId ?? "");
    setTransferMessage(null);
  }

  function openLinkPanel(studentId: string) {
    setOpenPanel({ studentId, type: "link" });
    setLinkPhone("");
    setLinkRelation("MOTHER");
    setLinkMessage(null);
  }

  function closePanel() {
    setOpenPanel(null);
  }

  async function handleTransfer(studentId: string) {
    if (!transferClassId) return;
    setIsTransferring(true);
    try {
      await transferStudentClass(studentId, transferClassId);
      setOpenPanel(null);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setTransferMessage({
        studentId,
        text: apiError?.message ?? "Sinfni almashtirib bo'lmadi.",
        isError: true,
      });
    } finally {
      setIsTransferring(false);
    }
  }

  async function handleLinkParent(studentId: string) {
    setIsLinking(true);
    setLinkMessage(null);
    try {
      await linkParentToStudent({ parentPhone: linkPhone, studentId, relation: linkRelation });
      setLinkMessage({ studentId, text: "Ota-ona muvaffaqiyatli bog'landi.", isError: false });
      setLinkPhone("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setLinkMessage({
        studentId,
        text: apiError?.message ?? "Ota-onani bog'lab bo'lmadi.",
        isError: true,
      });
    } finally {
      setIsLinking(false);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
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
            <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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
              <div className="flex items-end sm:col-span-2 lg:col-span-4">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Qo'shilmoqda..." : "O'quvchi qo'shish"}
                </Button>
              </div>
            </form>
            {error ? <p className="mt-3 text-sm text-destructive">{error}</p> : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Ro&apos;yxat</CardTitle>
            <form onSubmit={handleSearch} className="mt-2 flex flex-wrap items-end gap-2">
              <FormField label="Qidirish" htmlFor="search" className="w-64">
                <input
                  id="search"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Ism yoki familiya"
                  className={`${fieldClass} w-full`}
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
              <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">Ism familiya</th>
                    <th className="py-2 font-medium">Telefon</th>
                    <th className="py-2 font-medium">Sinf</th>
                    <th className="py-2 font-medium">Holati</th>
                    <th className="py-2 font-medium">Yozuv profili</th>
                    <th className="py-2 font-medium">Amallar</th>
                  </tr>
                </thead>
                <tbody>
                  {students.map((student) => {
                    const isTransferOpen =
                      openPanel?.studentId === student.id && openPanel.type === "transfer";
                    const isLinkOpen =
                      openPanel?.studentId === student.id && openPanel.type === "link";
                    return (
                      <Fragment key={student.id}>
                        <tr className="border-b border-border last:border-0">
                          <td className="py-2.5 font-medium">
                            {student.firstName} {student.lastName}
                          </td>
                          <td className="py-2.5 font-data text-muted-foreground">
                            {student.phone}
                          </td>
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
                                {unlockingId === student.id
                                  ? "Tiklanmoqda..."
                                  : "Reset limitini tiklash"}
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
                          <td className="py-2.5">
                            <div className="flex flex-wrap gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => openTransferPanel(student)}
                              >
                                <Users className="size-3.5" strokeWidth={1.75} />
                                Sinfni almashtirish
                              </Button>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => openLinkPanel(student.id)}
                              >
                                <Link2 className="size-3.5" strokeWidth={1.75} />
                                Ota-onani bog&apos;lash
                              </Button>
                            </div>
                          </td>
                        </tr>
                        {isTransferOpen ? (
                          <tr className="border-b border-border bg-muted/30">
                            <td colSpan={6} className="py-3">
                              <div className="flex flex-wrap items-end gap-3">
                                <FormField
                                  label="Yangi sinf"
                                  htmlFor={`transfer-class-${student.id}`}
                                  className="w-48"
                                >
                                  <SelectField
                                    id={`transfer-class-${student.id}`}
                                    value={transferClassId}
                                    onChange={(e) => setTransferClassId(e.target.value)}
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
                                <Button
                                  type="button"
                                  size="sm"
                                  disabled={isTransferring || !transferClassId}
                                  onClick={() => handleTransfer(student.id)}
                                >
                                  {isTransferring ? "Almashtirilmoqda..." : "Tasdiqlash"}
                                </Button>
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  disabled={isTransferring}
                                  onClick={closePanel}
                                >
                                  Bekor qilish
                                </Button>
                                {transferMessage?.studentId === student.id ? (
                                  <span
                                    className={`text-xs ${transferMessage.isError ? "text-destructive" : "text-success"}`}
                                  >
                                    {transferMessage.text}
                                  </span>
                                ) : null}
                              </div>
                            </td>
                          </tr>
                        ) : null}
                        {isLinkOpen ? (
                          <tr className="border-b border-border bg-muted/30">
                            <td colSpan={6} className="py-3">
                              <div className="flex flex-wrap items-end gap-3">
                                <FormField
                                  label="Ota-ona telefon raqami"
                                  htmlFor={`link-phone-${student.id}`}
                                  className="w-56"
                                >
                                  <input
                                    id={`link-phone-${student.id}`}
                                    type="tel"
                                    placeholder="+998901234567"
                                    value={linkPhone}
                                    onChange={(e) => setLinkPhone(e.target.value)}
                                    className={`${fieldClass} w-full`}
                                  />
                                </FormField>
                                <FormField
                                  label="Qarindoshlik"
                                  htmlFor={`link-relation-${student.id}`}
                                  className="w-36"
                                >
                                  <SelectField
                                    id={`link-relation-${student.id}`}
                                    value={linkRelation}
                                    onChange={(e) =>
                                      setLinkRelation(e.target.value as ParentRelation)
                                    }
                                  >
                                    {(Object.keys(RELATION_LABELS) as ParentRelation[]).map(
                                      (relation) => (
                                        <option key={relation} value={relation}>
                                          {RELATION_LABELS[relation]}
                                        </option>
                                      ),
                                    )}
                                  </SelectField>
                                </FormField>
                                <Button
                                  type="button"
                                  size="sm"
                                  disabled={isLinking || !linkPhone}
                                  onClick={() => handleLinkParent(student.id)}
                                >
                                  {isLinking ? "Bog'lanmoqda..." : "Bog'lash"}
                                </Button>
                                <Button
                                  type="button"
                                  variant="ghost"
                                  size="sm"
                                  disabled={isLinking}
                                  onClick={closePanel}
                                >
                                  Bekor qilish
                                </Button>
                                {linkMessage?.studentId === student.id ? (
                                  <span
                                    className={`text-xs ${linkMessage.isError ? "text-destructive" : "text-success"}`}
                                  >
                                    {linkMessage.text}
                                  </span>
                                ) : null}
                              </div>
                            </td>
                          </tr>
                        ) : null}
                      </Fragment>
                    );
                  })}
                </tbody>
              </table>
              </div>
            )}
            {!isLoading ? (
              <PaginationControl
                className="mt-4"
                page={studentsPage}
                size={studentsPageSize}
                totalItems={studentsTotalItems}
                onPageChange={(page) => {
                  setStudentsPage(page).catch(() =>
                    setError("O'quvchilar ro'yxatini yuklab bo'lmadi."),
                  );
                }}
                onSizeChange={(size) => {
                  setStudentsPageSize(size).catch(() =>
                    setError("O'quvchilar ro'yxatini yuklab bo'lmadi."),
                  );
                }}
              />
            ) : null}
          </CardContent>
        </Card>
      </div>
    </DashboardShell>
  );
}
