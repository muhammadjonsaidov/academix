"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { GraduationCap } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { useAuthStore } from "@/stores/useAuthStore";
import type { Role } from "@/types/auth";

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
    } catch {
      setError("Telefon raqam yoki parol noto'g'ri.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen flex-1 items-center justify-center bg-background p-4">
      <div className="animate-rise w-full max-w-sm">
        <div className="mb-6 flex flex-col items-center gap-2 text-center">
          <span className="flex size-11 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <GraduationCap className="size-6" strokeWidth={1.75} />
          </span>
          <h1 className="font-heading text-xl font-semibold">AcademiX AI</h1>
          <p className="text-sm text-muted-foreground">Ta&apos;lim platformasiga xush kelibsiz</p>
        </div>

        <Card>
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
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
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
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                />
              </div>

              {error ? (
                <p role="alert" className="text-sm text-destructive">
                  {error}
                </p>
              ) : null}

              <Button type="submit" className="w-full" size="lg" disabled={isSubmitting}>
                {isSubmitting ? "Kirilmoqda..." : "Kirish"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
