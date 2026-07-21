"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { KeyRound, Menu, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { NotificationBell } from "@/components/shared/NotificationBell";
import { TelegramConnect } from "@/components/shared/TelegramConnect";
import {
  NAV_CONFIG,
  ROLE_ACCENT_CLASSES,
  isNavItemActive,
  type NavGroup,
} from "@/components/shared/nav-config";
import { cn } from "@/lib/utils";
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

// External API (role/children props) is unchanged from the Sprint-1 shell — every
// app/dashboard/**/page.tsx keeps compiling untouched. This adds a persistent sidebar
// (desktop, >=1024px) / off-canvas drawer (mobile) built from NAV_CONFIG, on top of the
// same header (TelegramConnect/NotificationBell/logout) and the same auth-guard pattern.
export function DashboardShell({ role, children }: DashboardShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

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

  useEffect(() => {
    if (!isDrawerOpen) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setIsDrawerOpen(false);
    }
    document.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [isDrawerOpen]);

  if (!user) {
    return null;
  }

  function handleLogout() {
    logout();
    router.push("/login");
  }

  const groups = NAV_CONFIG[role];

  return (
    <div className="flex min-h-screen bg-background">
      {/* Desktop persistent sidebar */}
      <aside className="hidden lg:flex lg:w-sidebar lg:shrink-0 lg:flex-col lg:border-r lg:border-sidebar-border lg:bg-sidebar">
        <SidebarBrand role={role} />
        <SidebarNav role={role} groups={groups} pathname={pathname} />
      </aside>

      {/* Mobile off-canvas drawer — always mounted so both the slide-in and slide-out
          transitions actually animate, visibility/interactivity toggled via classes. */}
      <div
        className={cn("fixed inset-0 z-50 lg:hidden", isDrawerOpen ? "" : "pointer-events-none")}
        aria-hidden={!isDrawerOpen}
      >
        <div
          className={cn(
            "absolute inset-0 bg-foreground/40 backdrop-blur-sm transition-opacity duration-200",
            isDrawerOpen ? "opacity-100" : "opacity-0",
          )}
          onClick={() => setIsDrawerOpen(false)}
        />
        <aside
          className={cn(
            "relative z-10 flex h-full w-64 flex-col bg-sidebar shadow-xl transition-transform duration-200 ease-out",
            isDrawerOpen ? "translate-x-0" : "-translate-x-full",
          )}
        >
          <div className="flex items-center justify-between border-b border-sidebar-border px-4 py-4">
            <SidebarBrand role={role} compact onClick={() => setIsDrawerOpen(false)} />
            <Button
              variant="ghost"
              size="icon"
              aria-label="Menyuni yopish"
              onClick={() => setIsDrawerOpen(false)}
            >
              <X className="size-4" strokeWidth={1.75} />
            </Button>
          </div>
          <SidebarNav
            role={role}
            groups={groups}
            pathname={pathname}
            onNavigate={() => setIsDrawerOpen(false)}
          />
        </aside>
      </div>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between gap-3 border-b border-border px-4 py-4 lg:px-6">
          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              size="icon"
              className="lg:hidden"
              aria-label="Menyuni ochish"
              onClick={() => setIsDrawerOpen(true)}
            >
              <Menu className="size-4" strokeWidth={1.75} />
            </Button>
            <div>
              <p className="text-sm text-muted-foreground">{ROLE_LABEL[role]} paneli</p>
              <h1 className="font-heading text-lg font-semibold">
                {user.firstName} {user.lastName}
              </h1>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <TelegramConnect />
            <NotificationBell />
            <Button
              variant="outline"
              size="icon"
              aria-label="Hisob sozlamalari"
              render={<Link href="/dashboard/account" />}
            >
              <KeyRound className="size-4" strokeWidth={1.75} />
            </Button>
            <Button variant="outline" onClick={handleLogout}>
              Chiqish
            </Button>
          </div>
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}

function SidebarBrand({
  role,
  compact,
  onClick,
}: {
  role: Role;
  compact?: boolean;
  onClick?: () => void;
}) {
  const accent = ROLE_ACCENT_CLASSES[role];
  const rootHref = NAV_CONFIG[role][0]?.items[0]?.href ?? "/dashboard";

  return (
    <Link
      href={rootHref}
      onClick={onClick}
      className={cn(
        "flex items-center gap-2.5 px-4",
        compact ? "" : "border-b border-sidebar-border py-4",
      )}
    >
      <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground">
        A
      </span>
      <span className="flex flex-col leading-tight">
        <span className="font-heading text-sm font-semibold text-sidebar-foreground">
          AcademiX AI
        </span>
        <Badge variant={accent.badge} className="mt-0.5 w-fit">
          {ROLE_LABEL[role]}
        </Badge>
      </span>
    </Link>
  );
}

function SidebarNav({
  role,
  groups,
  pathname,
  onNavigate,
}: {
  role: Role;
  groups: NavGroup[];
  pathname: string;
  onNavigate?: () => void;
}) {
  const accent = ROLE_ACCENT_CLASSES[role];

  return (
    <nav className="flex-1 overflow-y-auto px-3 py-4">
      {groups.map((group, groupIndex) => (
        <div key={group.label ?? `group-${groupIndex}`}>
          {group.label ? (
            <p className="mt-4 mb-1 px-3 text-xs font-medium tracking-wide text-muted-foreground/70 uppercase first:mt-0">
              {group.label}
            </p>
          ) : null}
          <ul className="space-y-0.5">
            {group.items.map((item) => {
              const active = isNavItemActive(pathname, item.href);
              const Icon = item.icon;
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    onClick={onNavigate}
                    className={cn(
                      "flex items-center gap-2.5 rounded-md border-l-2 border-transparent px-3 py-2 text-sm font-medium transition-colors",
                      active ? accent.active : "text-muted-foreground hover:bg-muted/60",
                      active ? accent.border : undefined,
                    )}
                  >
                    <Icon className="size-4 shrink-0" strokeWidth={1.75} />
                    <span className="truncate">{item.label}</span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </nav>
  );
}
