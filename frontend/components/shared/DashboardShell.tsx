"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { LogOut, Menu, Settings, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { NotificationBell } from "@/components/shared/NotificationBell";
import { TelegramConnect } from "@/components/shared/TelegramConnect";
import { ThemeToggle } from "@/components/shared/ThemeToggle";
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
  const bootstrapSession = useAuthStore((state) => state.bootstrapSession);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  // A hard reload wipes the in-memory session but keeps the httpOnly refresh cookie — try to
  // silently restore before giving up and redirecting to login (this used to be an
  // unconditional redirect, i.e. every F5 logged the user out).
  //
  // Navigation must happen in an effect, not directly in the render body — a real React
  // error caught by an actual browser test, not a lint rule: "Cannot update a component
  // (Router) while rendering a different component (DashboardShell)."
  useEffect(() => {
    if (user) return;
    let cancelled = false;
    bootstrapSession().then((restored) => {
      if (!restored && !cancelled) {
        router.replace("/login");
      }
    });
    return () => {
      cancelled = true;
    };
  }, [user, router, bootstrapSession]);

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
    // Session bootstrap in flight — neutral blank shell beats a login flash for the common
    // "restores fine" case.
    return <div className="min-h-screen bg-background" />;
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
            <ThemeToggle />
            <TelegramConnect />
            <NotificationBell />
            <UserMenu
              role={role}
              firstName={user.firstName}
              lastName={user.lastName}
              onLogout={handleLogout}
            />
          </div>
        </header>
        {/* Keyed by pathname so client-side route changes re-run the rise
            animation — the new page settles in instead of hard-swapping. */}
        <main key={pathname} className="animate-rise flex-1 p-6">
          {children}
        </main>
      </div>
    </div>
  );
}

function UserMenu({
  role,
  firstName,
  lastName,
  onLogout,
}: {
  role: Role;
  firstName: string;
  lastName: string;
  onLogout: () => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const initials = `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase() || "A";

  useEffect(() => {
    if (!isOpen) return;
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-label="Profil menyusi"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((open) => !open)}
        className="flex size-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground outline-none transition-transform focus-visible:ring-3 focus-visible:ring-ring/50 active:translate-y-px"
      >
        {initials}
      </button>
      {isOpen && (
        <div className="animate-rise absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-lg border border-border bg-card shadow-lg">
          <div className="border-b border-border px-3.5 py-2.5">
            <p className="truncate text-sm font-medium">
              {firstName} {lastName}
            </p>
            <p className="text-xs text-muted-foreground">{ROLE_LABEL[role]}</p>
          </div>
          <div className="p-1.5">
            <Link
              href="/dashboard/account"
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm transition-colors hover:bg-muted"
            >
              <Settings className="size-4 text-muted-foreground" strokeWidth={1.75} />
              Profil sozlamalari
            </Link>
            <button
              type="button"
              onClick={onLogout}
              className="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left text-sm transition-colors hover:bg-muted"
            >
              <LogOut className="size-4 text-muted-foreground" strokeWidth={1.75} />
              Chiqish
            </button>
          </div>
        </div>
      )}
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
