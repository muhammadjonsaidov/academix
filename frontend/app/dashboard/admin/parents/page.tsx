"use client";

import { Fragment, useEffect, useState, type FormEvent } from "react";
import { Link2, UserPlus, Users } from "lucide-react";
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
import type { ParentRelation } from "@/types/admin";
import type { ApiErrorResponse } from "@/types/auth";

const RELATION_LABELS: Record<ParentRelation, string> = {
  MOTHER: "Ona",
  FATHER: "Ota",
  GUARDIAN: "Vasiy",
};

export default function AdminParentsPage() {
  const parents = useAdminStore((state) => state.parents);
  const allStudents = useAdminStore((state) => state.allStudents);
  const fetchParents = useAdminStore((state) => state.fetchParents);
  const fetchAllStudents = useAdminStore((state) => state.fetchAllStudents);
  const createParent = useAdminStore((state) => state.createParent);
  const linkParentToStudent = useAdminStore((state) => state.linkParentToStudent);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // Per-parent inline link form state.
  const [linkingParentId, setLinkingParentId] = useState<string | null>(null);
  const [linkStudentId, setLinkStudentId] = useState("");
  const [linkRelation, setLinkRelation] = useState<ParentRelation>("MOTHER");
  const [linkError, setLinkError] = useState<string | null>(null);
  const [isLinking, setIsLinking] = useState(false);

  useEffect(() => {
    Promise.all([fetchParents(), fetchAllStudents()])
      .catch(() => setFormError("Ota-onalar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchParents, fetchAllStudents]);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const policy = validatePassword(password);
    if (!policy.valid) {
      setFormError(policy.error);
      return;
    }

    setIsSubmitting(true);
    try {
      await createParent({ firstName, lastName, phone, email: email || undefined, password });
      setFirstName("");
      setLastName("");
      setPhone("");
      setEmail("");
      setPassword("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setFormError(apiError?.message ?? "Ota-onani qo'shib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleLink(parentPhone: string) {
    if (!linkStudentId) {
      setLinkError("O'quvchini tanlang.");
      return;
    }
    setLinkError(null);
    setIsLinking(true);
    try {
      await linkParentToStudent({
        parentPhone,
        studentId: linkStudentId,
        relation: linkRelation,
      });
      setLinkingParentId(null);
      setLinkStudentId("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setLinkError(apiError?.message ?? "Bog'lab bo'lmadi.");
    } finally {
      setIsLinking(false);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <PageHeader
          title="Ota-onalar / Vasiylar"
          description="Ota-ona hisoblarini yarating, boshlang'ich parol bering va farzandlariga bog'lang. Ular keyin parolni o'zlari almashtirishi mumkin."
          eyebrow="Hamkorlik"
        />

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="size-4" strokeWidth={1.75} />
              Yangi ota-ona / vasiy qo&apos;shish
            </CardTitle>
            <CardDescription>
              Telefon va boshlang&apos;ich parol majburiy, email ixtiyoriy (lekin parolni tiklash
              email orqali ishlaydi). Parolni ota-onaga o&apos;zingiz yetkazasiz.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreate} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <FormField label="Ism" htmlFor="parentFirstName">
                <input
                  id="parentFirstName"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Familiya" htmlFor="parentLastName">
                <input
                  id="parentLastName"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Telefon raqam" htmlFor="parentPhone">
                <input
                  id="parentPhone"
                  type="tel"
                  placeholder="+998901234567"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Email (ixtiyoriy)" htmlFor="parentEmail">
                <input
                  id="parentEmail"
                  type="email"
                  placeholder="ota-ona@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Boshlang'ich parol" htmlFor="parentPassword">
                <input
                  id="parentPassword"
                  type="text"
                  minLength={8}
                  placeholder="Kamida 8 belgi"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <div className="flex items-end">
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Qo'shilmoqda..." : "Qo'shish"}
                </Button>
              </div>
            </form>
            {formError ? <p className="mt-3 text-sm text-destructive">{formError}</p> : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Ro&apos;yxat</CardTitle>
            <CardDescription>
              Har bir ota-onaning bog&apos;langan farzandlari shu yerda ko&apos;rinadi.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : parents.length === 0 ? (
              <EmptyState
                icon={Users}
                title="Hozircha ota-onalar yo'q"
                description="Yuqoridagi shakl orqali birinchi ota-onani qo'shing."
              />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-muted-foreground">
                    <tr className="border-b border-border">
                      <th className="py-2 font-medium">Ism familiya</th>
                      <th className="py-2 font-medium">Telefon</th>
                      <th className="py-2 font-medium">Email</th>
                      <th className="py-2 font-medium">Farzandlari</th>
                      <th className="py-2"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {parents.map((parent) => (
                      <Fragment key={parent.id}>
                        <tr className="border-b border-border last:border-0">
                          <td className="py-2.5 font-medium">
                            {parent.firstName} {parent.lastName}
                          </td>
                          <td className="py-2.5 font-data text-muted-foreground">
                            {parent.phone}
                          </td>
                          <td className="py-2.5 text-muted-foreground">{parent.email ?? "—"}</td>
                          <td className="py-2.5">
                            {parent.children.length === 0 ? (
                              <span className="text-muted-foreground">
                                Hali bog&apos;lanmagan
                              </span>
                            ) : (
                              <div className="flex flex-wrap gap-1.5">
                                {parent.children.map((child) => (
                                  <Badge key={child.studentId} variant="secondary">
                                    {child.firstName} {child.lastName}
                                    {child.className ? ` · ${child.className}` : ""}
                                    {` · ${RELATION_LABELS[child.relation]}`}
                                  </Badge>
                                ))}
                              </div>
                            )}
                          </td>
                          <td className="py-2.5 text-right">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setLinkError(null);
                                setLinkStudentId("");
                                setLinkingParentId(
                                  linkingParentId === parent.id ? null : parent.id,
                                );
                              }}
                            >
                              <Link2 className="mr-1 size-3.5" strokeWidth={1.75} />
                              Farzand bog&apos;lash
                            </Button>
                          </td>
                        </tr>
                        {linkingParentId === parent.id ? (
                          <tr className="border-b border-border">
                            <td colSpan={5} className="bg-muted/40 px-3 py-3">
                              <div className="flex flex-wrap items-end gap-3">
                                <FormField label="O'quvchi" htmlFor={`link-student-${parent.id}`}>
                                  <select
                                    id={`link-student-${parent.id}`}
                                    value={linkStudentId}
                                    onChange={(e) => setLinkStudentId(e.target.value)}
                                    className={`${fieldClass} min-w-56`}
                                  >
                                    <option value="">Tanlang...</option>
                                    {allStudents
                                      .filter(
                                        (s) =>
                                          !parent.children.some((c) => c.studentId === s.id),
                                      )
                                      .map((s) => (
                                        <option key={s.id} value={s.id}>
                                          {s.firstName} {s.lastName}
                                        </option>
                                      ))}
                                  </select>
                                </FormField>
                                <FormField
                                  label="Qarindoshlik"
                                  htmlFor={`link-relation-${parent.id}`}
                                >
                                  <select
                                    id={`link-relation-${parent.id}`}
                                    value={linkRelation}
                                    onChange={(e) =>
                                      setLinkRelation(e.target.value as ParentRelation)
                                    }
                                    className={fieldClass}
                                  >
                                    {(
                                      Object.keys(RELATION_LABELS) as ParentRelation[]
                                    ).map((rel) => (
                                      <option key={rel} value={rel}>
                                        {RELATION_LABELS[rel]}
                                      </option>
                                    ))}
                                  </select>
                                </FormField>
                                <Button
                                  size="sm"
                                  disabled={isLinking}
                                  onClick={() => handleLink(parent.phone)}
                                >
                                  {isLinking ? "Bog'lanmoqda..." : "Bog'lash"}
                                </Button>
                              </div>
                              {linkError ? (
                                <p className="mt-2 text-sm text-destructive">{linkError}</p>
                              ) : null}
                            </td>
                          </tr>
                        ) : null}
                      </Fragment>
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
