"use client";

import { useState, type FormEvent } from "react";
import { CheckCircle2, KeyRound } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuthStore } from "@/stores/useAuthStore";
import type { ApiErrorResponse, Role } from "@/types/auth";

// Not under any role-prefixed folder (/dashboard/{admin,teacher,...}/) — proxy.ts's
// route-guard only enforces role-prefix matching for those literal segments, so this page
// works for whichever role is currently logged in, driven off useAuthStore.user.role at
// render time rather than a hardcoded role prop.
export default function AccountPage() {
  const user = useAuthStore((state) => state.user);
  const changePassword = useAuthStore((state) => state.changePassword);

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const role: Role = user?.role ?? "STUDENT";
  const mismatch = confirmPassword.length > 0 && newPassword !== confirmPassword;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaved(false);

    if (newPassword !== confirmPassword) {
      setError("Yangi parollar bir xil emas.");
      return;
    }

    setIsSubmitting(true);
    try {
      await changePassword(oldPassword, newPassword);
      setSaved(true);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Parolni o'zgartirib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <DashboardShell role={role}>
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Hisob</h2>
          <p className="text-sm text-muted-foreground">Parolingizni o&apos;zgartiring.</p>
        </div>

        <Card className="max-w-md">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <KeyRound className="size-4" strokeWidth={1.75} />
              Parolni o&apos;zgartirish
            </CardTitle>
            {saved ? (
              <CardDescription className="flex items-center gap-1.5 text-success">
                <CheckCircle2 className="size-3.5" strokeWidth={1.75} />
                Parol muvaffaqiyatli o&apos;zgartirildi.
              </CardDescription>
            ) : null}
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-3">
              <FormField label="Joriy parol" htmlFor="oldPassword">
                <input
                  id="oldPassword"
                  type="password"
                  autoComplete="current-password"
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Yangi parol" htmlFor="newPassword">
                <input
                  id="newPassword"
                  type="password"
                  autoComplete="new-password"
                  minLength={8}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              <FormField label="Yangi parolni tasdiqlang" htmlFor="confirmPassword">
                <input
                  id="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  minLength={8}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  aria-invalid={mismatch}
                  className={`${fieldClass} w-full`}
                />
              </FormField>
              {mismatch ? (
                <p className="text-sm text-destructive">Yangi parollar bir xil emas.</p>
              ) : null}
              {error ? <p className="text-sm text-destructive">{error}</p> : null}
              <Button type="submit" disabled={isSubmitting || mismatch}>
                {isSubmitting ? "Saqlanmoqda..." : "Parolni yangilash"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </DashboardShell>
  );
}
