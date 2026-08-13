"use client";

import { useState, type FormEvent } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Eye, EyeOff, Sparkles } from "lucide-react";
import { fieldClass } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { useAuthStore } from "@/stores/useAuthStore";
import type { ApiErrorResponse, Role } from "@/types/auth";

const ROLE_DASHBOARD_PATH: Record<Role, string> = {
  ADMIN: "/dashboard/admin",
  TEACHER: "/dashboard/teacher",
  STUDENT: "/dashboard/student",
  PARENT: "/dashboard/parent",
  PSYCHOLOGIST: "/dashboard/psychologist",
};

export default function LoginPage() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await login(phone, password);
      const role = useAuthStore.getState().user?.role;
      router.push(role ? ROLE_DASHBOARD_PATH[role] : "/dashboard");
    } catch (err) {
      // Only treat a real backend error as wrong credentials. A network failure or a
      // non-JSON response (e.g. an HTML 404 when the API base URL is misconfigured)
      // used to be swallowed into the same message, which made a broken frontend↔backend
      // connection look like a wrong password — that exact confusion is what this guard fixes.
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(
        apiError?.message ??
          "Serverga ulanib bo'lmadi. Tarmoqni tekshirib qayta urinib ko'ring.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="relative isolate flex min-h-screen flex-1 items-center justify-center overflow-hidden bg-background p-4">
      {/* Ambient AI + chalk glows — same language as the landing hero */}
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
            <Sparkles className="size-3" strokeWidth={1.75} />
            AI baholash platformasi
          </span>
        </div>

        <Card className="shadow-lg">
          <CardHeader className="border-b-0 pb-0">
            <p className="text-sm font-medium text-foreground">Tizimga kirish</p>
            <p className="text-sm text-muted-foreground">
              Telefon raqamingiz va parolingizni kiriting
            </p>
          </CardHeader>
          <CardContent className="pt-4">
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
                  className={`${fieldClass} w-full`}
                />
              </div>

              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="password" className="text-sm font-medium">
                    Parol
                  </label>
                  <Link
                    href="/forgot-password"
                    className="text-sm text-primary underline-offset-4 hover:underline"
                  >
                    Parolni unutdingizmi?
                  </Link>
                </div>
                <div className="relative">
                  <input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    autoComplete="current-password"
                    className={`${fieldClass} w-full pr-9`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? "Parolni yashirish" : "Parolni ko'rsatish"}
                    aria-pressed={showPassword}
                    className="absolute top-1/2 right-1.5 flex size-7 -translate-y-1/2 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors outline-none hover:bg-muted/60 hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50"
                  >
                    {showPassword ? (
                      <EyeOff className="size-4" strokeWidth={1.75} />
                    ) : (
                      <Eye className="size-4" strokeWidth={1.75} />
                    )}
                  </button>
                </div>
              </div>

              {error ? (
                <p role="alert" className="text-sm text-destructive">
                  {error}
                </p>
              ) : null}

              <Button
                type="submit"
                variant="gradient"
                className="w-full"
                size="lg"
                disabled={isSubmitting}
              >
                {isSubmitting ? "Kirilmoqda..." : "Kirish"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
