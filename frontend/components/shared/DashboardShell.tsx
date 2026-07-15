"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/useAuthStore";
import type { Role } from "@/types/auth";

const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "Administrator",
  TEACHER: "O'qituvchi",
  STUDENT: "O'quvchi",
  PARENT: "Ota-ona",
  PSYCHOLOGIST: "Psixolog",
};

interface DashboardShellProps {
  role: Role;
  children?: React.ReactNode;
}

// Sprint 1 scope: empty shell per role, proving the auth+route-guard chain end to end.
// Real dashboard content (pending homework, class analytics, etc.) is later sprints' work.
export function DashboardShell({ role, children }: DashboardShellProps) {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);

  // In-memory-only store means a fresh tab/hard reload loses the session even with a still-
  // valid httpOnly cookie (proxy.ts would have already let the request through) — known gap,
  // see CLAUDE.md "Reality checks". Redirect to login rather than render a broken shell.
  //
  // Navigation must happen in an effect, not directly in the render body — a real React
  // error caught by an actual browser test, not a lint rule: "Cannot update a component
  // (Router) while rendering a different component (DashboardShell)."
  useEffect(() => {
    if (!user) {
      router.replace("/login");
    }
  }, [user, router]);

  if (!user) {
    return null;
  }

  function handleLogout() {
    logout();
    router.push("/login");
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="flex items-center justify-between border-b border-border px-6 py-4">
        <div>
          <p className="text-sm text-muted-foreground">{ROLE_LABEL[role]} paneli</p>
          <h1 className="text-lg font-semibold">
            {user.firstName} {user.lastName}
          </h1>
        </div>
        <Button variant="outline" onClick={handleLogout}>
          Chiqish
        </Button>
      </header>
      <main className="p-6">{children}</main>
    </div>
  );
}
