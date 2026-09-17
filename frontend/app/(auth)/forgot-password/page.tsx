"use client";

import { useState, type FormEvent } from "react";
import Image from "next/image";
import Link from "next/link";
import { KeyRound } from "lucide-react";
import { fieldClass } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { useAuthStore } from "@/stores/useAuthStore";

export default function ForgotPasswordPage() {
  const forgotPassword = useAuthStore((state) => state.forgotPassword);
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      const result = await forgotPassword(email);
      setMessage(result);
    } catch {
      // Backend always returns success (anti-enumeration) — a network/500 failure here is
      // the only realistic non-2xx case, so this generic fallback matches the same tone.
      setMessage("So'rovni yuborib bo'lmadi. Birozdan so'ng qayta urinib ko'ring.");
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
            Parolni tiklash
          </span>
        </div>

        <Card className="shadow-lg">
          <CardHeader className="border-b-0 pb-0">
            <p className="flex items-center gap-2 text-sm font-medium text-foreground">
              <KeyRound className="size-4" strokeWidth={1.75} />
              Parolni tiklash
            </p>
            <p className="text-sm text-muted-foreground">
              Hisobingizga biriktirilgan emailni kiriting. Tiklash havolasi shu manzilga yuboriladi.
            </p>
          </CardHeader>
          <CardContent className="pt-4">
            {message ? (
              <div className="space-y-4">
                <p className="text-sm text-foreground">{message}</p>
                <Link
                  href="/login"
                  className="text-sm text-primary underline-offset-4 hover:underline"
                >
                  Kirish sahifasiga qaytish
                </Link>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-1.5">
                  <label htmlFor="email" className="text-sm font-medium">
                    Email manzil
                  </label>
                  <input
                    id="email"
                    type="email"
                    placeholder="admin@maktab.uz"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    autoComplete="email"
                    required
                    className={`${fieldClass} w-full`}
                  />
                </div>
                <Button type="submit" variant="gradient" className="w-full" size="lg" disabled={isSubmitting}>
                  {isSubmitting ? "Yuborilmoqda..." : "Tiklash havolasini yuborish"}
                </Button>
                <Link
                  href="/login"
                  className="block text-center text-sm text-muted-foreground underline-offset-4 hover:underline"
                >
                  Kirish sahifasiga qaytish
                </Link>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
