"use client";

import { Menu } from "lucide-react";
import { NotificationBell } from "@/components/shared/NotificationBell";
import { TelegramConnect } from "@/components/shared/TelegramConnect";
import { ThemeToggle } from "@/components/shared/ThemeToggle";
import { UserMenu } from "@/components/shell/UserMenu";
import { ROLE_LABEL } from "@/components/shell/role";
import { Button } from "@/components/ui/button";
import type { Role } from "@/types/auth";

interface DashboardHeaderProps {
  role: Role;
  firstName: string;
  lastName: string;
  onOpenNavigation: () => void;
  onLogout: () => void;
}

/** Persistent contextual header; page titles intentionally remain in each route's main content. */
export function DashboardHeader({
  role,
  firstName,
  lastName,
  onOpenNavigation,
  onLogout,
}: DashboardHeaderProps) {
  return (
    <header className="flex items-center justify-between gap-2 border-b border-border px-3 py-3 sm:gap-3 sm:px-4 sm:py-4 lg:px-6">
      <div className="flex min-w-0 items-center gap-2 sm:gap-3">
        <Button
          variant="outline"
          size="icon"
          className="shrink-0 lg:hidden"
          aria-label="Menyuni ochish"
          onClick={onOpenNavigation}
        >
          <Menu className="size-4" strokeWidth={1.75} />
        </Button>
        <div className="flex min-w-0 items-center gap-2">
          <span
            aria-hidden
            className="bg-accent-role size-2 shrink-0 rounded-full ring-2 ring-[var(--accent-role)]/25"
          />
          <div className="min-w-0">
            <p className="hidden text-xs text-muted-foreground sm:block sm:text-sm">
              {ROLE_LABEL[role]} paneli
            </p>
            <p className="truncate text-base leading-tight font-semibold sm:text-lg">
              {firstName} {lastName}
            </p>
          </div>
        </div>
      </div>
      <div className="flex shrink-0 items-center gap-1 sm:gap-2 lg:gap-3">
        <ThemeToggle />
        <TelegramConnect />
        <NotificationBell />
        <UserMenu role={role} firstName={firstName} lastName={lastName} onLogout={onLogout} />
      </div>
    </header>
  );
}
