"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { DashboardHeader } from "@/components/shell/DashboardHeader";
import { MobileNavigation } from "@/components/shell/MobileNavigation";
import { SidebarBrand } from "@/components/shell/SidebarBrand";
import { SidebarNavigation } from "@/components/shell/SidebarNavigation";
import { dashboardPageTitle } from "@/components/shared/nav-config";
import { useRealtimeEvents } from "@/hooks/useRealtime";
import { useAuthStore } from "@/stores/useAuthStore";
import type { Role } from "@/types/auth";

interface DashboardShellProps {
  role: Role;
  children?: React.ReactNode;
}

/**
 * Application-frame composition root. It owns session restoration and responsive navigation;
 * route pages only own their feature-specific data and interactions.
 */
export function DashboardShell({ role, children }: DashboardShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const bootstrapSession = useAuthStore((state) => state.bootstrapSession);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const pageTitle = dashboardPageTitle(role, pathname);

  useRealtimeEvents();

  useEffect(() => {
    if (user) return;
    let cancelled = false;
    bootstrapSession().then((restored) => {
      if (!restored && !cancelled) router.replace("/login");
    });
    return () => {
      cancelled = true;
    };
  }, [bootstrapSession, router, user]);

  useEffect(() => {
    document.title = `${pageTitle} · AcademiX AI`;
  }, [pageTitle]);

  if (!user) {
    return <div className="min-h-screen bg-background" />;
  }

  function handleLogout() {
    logout();
    router.push("/login");
  }

  return (
    <div data-role={role} className="relative flex min-h-screen bg-background">
      <a
        href="#dashboard-content"
        className="sr-only fixed top-3 left-3 z-[60] rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground focus:not-sr-only"
      >
        Asosiy mazmunga o&apos;tish
      </a>
      <div
        aria-hidden
        className="bg-gradient-to-r from-[var(--accent-role)] via-[var(--accent-role)] to-transparent shell-hairline pointer-events-none absolute top-0 right-0 left-0 z-50 h-0.5"
      />
      <aside className="hidden lg:flex lg:w-sidebar lg:shrink-0 lg:flex-col lg:border-r lg:border-sidebar-border lg:bg-sidebar">
        <SidebarBrand role={role} />
        <SidebarNavigation role={role} pathname={pathname} />
      </aside>
      <MobileNavigation
        role={role}
        pathname={pathname}
        open={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
      />
      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
        <DashboardHeader
          role={role}
          firstName={user.firstName}
          lastName={user.lastName}
          onOpenNavigation={() => setIsDrawerOpen(true)}
          onLogout={handleLogout}
        />
        <main
          id="dashboard-content"
          key={pathname}
          aria-label={pageTitle}
          className="animate-rise mx-auto flex w-full max-w-[90rem] flex-1 p-4 sm:p-6"
        >
          <div className="min-w-0 flex-1">{children}</div>
        </main>
      </div>
    </div>
  );
}
