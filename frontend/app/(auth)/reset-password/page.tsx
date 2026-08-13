"use client";

import { Suspense, useState, type FormEvent } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { KeyRound } from "lucide-react";
import { fieldClass } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { useAuthStore } from "@/stores/useAuthStore";

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ResetPasswordForm />
    </Suspense>
  );
}

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const resetPassword = useAuthStore((state) => state.resetPassword);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDone, setIsDone] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (newPassword !== confirmPassword) {
      setError("Parollar mos kelmadi.");
      return;
    }
    setIsSubmitting(true);
    try {
      await resetPassword(token, newPassword);
      setIsDone(true);
    } catch {
      setError("Havola yaroqsiz yoki muddati tugagan. Parolni tiklashni qaytadan so'rang.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="relative isolate flex min-h-screen flex-1 items-center justify-center overflow-hidden bg-background p-4">
      {/* Ambient AI + chalk glows — same language as the landing hero and login page */}
      <div aria-hidden className="absolute inset-0 -z-10">
        <div className="bg-ai-gradient absolute -top-32 left-1/2 size-[26rem] -translate-x-1/2 rounded-full opacity-15 blur-3xl" />
        <div className="absolute -bottom-24 -left-16 size-80 rounded-full bg-chalk-green/10 blur-3xl" />
      </div>

      <div className="animate-rise w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <Image src="/logo-mark.svg" alt="AcademiX AI" width={64} height={59} priority />
          <h1 className="font-heading text-xl font-semibold">
            AcademiX <span className="text-ai-gradient">AI</span>
          </h1>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-ai-soft bg-ai-soft/60 px-2.5 py-0.5 text-xs font-medium text-ai">
            <KeyRound className="size-3" strokeWidth={1.75} />
            Yangi parol
          </span>
        </div>

        <Card className="shadow-lg">
          <CardHeader className="border-b-0 pb-0">
            <p className="flex items-center gap-2 text-sm font-medium text-foreground">
              <KeyRound className="size-4" strokeWidth={1.75} />
              Yangi parol
            </p>
          </CardHeader>
          <CardContent className="pt-4">
            {isDone ? (
              <div className="space-y-4">
                <p className="text-sm text-foreground">
                  Parolingiz muvaffaqiyatli o&apos;zgartirildi.
                </p>
                <Button variant="gradient" className="w-full" onClick={() => router.push("/login")}>
                  Kirish sahifasiga o&apos;tish
                </Button>
              </div>
            ) : !token ? (
              <p className="text-sm text-destructive">
                Havola yaroqsiz — token topilmadi.{" "}
                <Link href="/forgot-password" className="underline-offset-4 hover:underline">
                  Qaytadan urinib ko&apos;ring
                </Link>
                .
              </p>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-1.5">
                  <label htmlFor="newPassword" className="text-sm font-medium">
                    Yangi parol
                  </label>
                  <input
                    id="newPassword"
                    type="password"
                    minLength={8}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    required
                    className={`${fieldClass} w-full`}
                  />
                </div>
                <div className="space-y-1.5">
                  <label htmlFor="confirmPassword" className="text-sm font-medium">
                    Yangi parolni tasdiqlang
                  </label>
                  <input
                    id="confirmPassword"
                    type="password"
                    minLength={8}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    className={`${fieldClass} w-full`}
                  />
                </div>
                {error ? (
                  <p role="alert" className="text-sm text-destructive">
                    {error}
                  </p>
                ) : null}
                <Button type="submit" variant="gradient" className="w-full" size="lg" disabled={isSubmitting}>
                  {isSubmitting ? "Saqlanmoqda..." : "Parolni saqlash"}
                </Button>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
