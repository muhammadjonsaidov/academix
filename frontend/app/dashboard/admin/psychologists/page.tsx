"use client";

import { useEffect, useState, type FormEvent } from "react";
import { HeartPulse, UserPlus } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

export default function AdminPsychologistsPage() {
  const psychologists = useAdminStore((state) => state.psychologists);
  const fetchPsychologists = useAdminStore((state) => state.fetchPsychologists);
  const invitePsychologist = useAdminStore((state) => state.invitePsychologist);
  const setPsychologistActive = useAdminStore((state) => state.setPsychologistActive);

  const [phone, setPhone] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [togglingId, setTogglingId] = useState<string | null>(null);

  useEffect(() => {
    fetchPsychologists()
      .catch(() => setError("Psixologlar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchPsychologists]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await invitePsychologist({ phone, firstName, lastName, email: email || undefined });
      setPhone("");
      setFirstName("");
      setLastName("");
      setEmail("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Psixolog taklif qilib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleToggleActive(psychologistId: string, active: boolean) {
    setTogglingId(psychologistId);
    try {
      await setPsychologistActive(psychologistId, active);
    } finally {
      setTogglingId(null);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Psixologlar</h2>
          <p className="text-sm text-muted-foreground">
            Maktabga psixolog taklif qiling va ularning holatini boshqaring.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UserPlus className="size-4" strokeWidth={1.75} />
              Yangi psixolog taklif qilish
            </CardTitle>
            <CardDescription>
              Taklif SMS orqali yuboriladi, psixolog birinchi kirishda parolini o&apos;rnatadi.
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
              <FormField label="Email (ixtiyoriy)" htmlFor="email">
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
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
            ) : psychologists.length === 0 ? (
              <EmptyState
                icon={HeartPulse}
                title="Hozircha psixologlar yo'q"
                description="Yuqoridagi shakl orqali birinchi psixologni taklif qiling."
              />
            ) : (
              <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">Ism familiya</th>
                    <th className="py-2 font-medium">Telefon</th>
                    <th className="py-2 font-medium">Email</th>
                    <th className="py-2 font-medium">Holati</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {psychologists.map((psychologist) => (
                    <tr key={psychologist.id} className="border-b border-border last:border-0">
                      <td className="py-2.5 font-medium">
                        {psychologist.firstName} {psychologist.lastName}
                      </td>
                      <td className="py-2.5 font-data text-muted-foreground">
                        {psychologist.phone}
                      </td>
                      <td className="py-2.5 text-muted-foreground">
                        {psychologist.email ?? "—"}
                      </td>
                      <td className="py-2.5">
                        <Badge variant={psychologist.isActive ? "success" : "secondary"}>
                          {psychologist.isActive ? "Faol" : "Faol emas"}
                        </Badge>
                      </td>
                      <td className="py-2.5 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={togglingId === psychologist.id}
                          onClick={() =>
                            handleToggleActive(psychologist.id, !psychologist.isActive)
                          }
                        >
                          {togglingId === psychologist.id
                            ? "..."
                            : psychologist.isActive
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
