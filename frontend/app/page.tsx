"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { GraduationCap } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/stores/useAuthStore";
import type { Role } from "@/types/auth";

const ROLE_DASHBOARD_PATH: Record<Role, string> = {
  ADMIN: "/dashboard/admin",
  TEACHER: "/dashboard/teacher",
  STUDENT: "/dashboard/student",
  PARENT: "/dashboard/parent",
  PSYCHOLOGIST: "/dashboard/psychologist",
};

// Public marketing/splash root ("/"). If a session already exists in memory (e.g.
// client-side nav from another tab of the same app), bounce straight to that role's
// dashboard instead of showing the splash — but never in the render body, always in an
// effect (see DashboardShell's comment on the same rule).
export default function HomePage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    if (user) {
      router.replace(ROLE_DASHBOARD_PATH[user.role]);
    }
  }, [user, router]);

  if (user) {
    return null;
  }

  return (
    <main className="flex flex-1 flex-col items-center justify-center bg-background px-4 py-24">
      <div className="flex max-w-lg flex-col items-center gap-6 text-center">
        <span className="flex size-14 items-center justify-center rounded-2xl bg-primary text-primary-foreground">
          <GraduationCap className="size-7" strokeWidth={1.75} />
        </span>
        <div className="flex flex-col gap-2">
          <h1 className="font-heading text-3xl font-semibold tracking-tight">AcademiX AI</h1>
          <p className="text-muted-foreground">
            Qo&apos;lyozma uy vazifalari va nazorat ishlarini sun&apos;iy intellekt yordamida
            baholovchi, o&apos;quvchilar rivojlanishini kuzatuvchi maktab platformasi.
          </p>
        </div>
        <Link href="/login" className={cn(buttonVariants({ size: "lg" }), "px-6")}>
          Tizimga kirish
        </Link>
      </div>
    </main>
  );
}
