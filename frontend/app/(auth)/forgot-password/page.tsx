"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { GraduationCap, KeyRound } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { useAuthStore } from "@/stores/useAuthStore";

export default function ForgotPasswordPage() {
  const forgotPassword = useAuthStore((state) => state.forgotPassword);
  const [phone, setPhone] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      const result = await forgotPassword(phone);
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
    <main className="flex min-h-screen flex-1 items-center justify-center bg-background p-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <span className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <GraduationCap className="size-6" strokeWidth={1.75} />
          </span>
          <h1 className="font-heading text-xl font-semibold">AcademiX AI</h1>
        </div>

        <Card>
          <CardHeader className="border-b-0 pb-0">
            <p className="flex items-center gap-2 text-sm font-medium text-foreground">
              <KeyRound className="size-4" strokeWidth={1.75} />
              Parolni tiklash
            </p>
            <p className="text-sm text-muted-foreground">
              Telefon raqamingizni kiriting — agar hisobingizga email biriktirilgan bo&apos;lsa,
              tiklash havolasi shu emailga yuboriladi.
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
                    className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                  />
                </div>
                <Button type="submit" className="w-full" size="lg" disabled={isSubmitting}>
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
