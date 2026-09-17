"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { LogOut, Settings } from "lucide-react";
import { ROLE_LABEL } from "@/components/shell/role";
import type { Role } from "@/types/auth";

interface UserMenuProps {
  role: Role;
  firstName: string;
  lastName: string;
  onLogout: () => void;
}

/** Small, keyboard-dismissible account menu owned by the app frame. */
export function UserMenu({ role, firstName, lastName, onLogout }: UserMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const initials = `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase() || "A";

  useEffect(() => {
    if (!isOpen) return;

    function closeMenu(event: KeyboardEvent) {
      if (event.key === "Escape") setIsOpen(false);
    }
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    document.addEventListener("keydown", closeMenu);
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("keydown", closeMenu);
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-label="Profil menyusi"
        aria-expanded={isOpen}
        aria-haspopup="menu"
        onClick={() => setIsOpen((open) => !open)}
        className="flex size-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-primary-foreground outline-none transition-transform focus-visible:ring-3 focus-visible:ring-ring/50 active:translate-y-px"
      >
        {initials}
      </button>
      {isOpen ? (
        <div
          role="menu"
          aria-label="Profil amallari"
          className="animate-rise absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-lg border border-border bg-card shadow-lg"
        >
          <div className="border-b border-border px-3.5 py-2.5">
            <p className="truncate text-sm font-medium">
              {firstName} {lastName}
            </p>
            <p className="text-xs text-muted-foreground">{ROLE_LABEL[role]}</p>
          </div>
          <div className="p-1.5">
            <Link
              href="/dashboard/account"
              role="menuitem"
              onClick={() => setIsOpen(false)}
              className="flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm transition-colors hover:bg-muted"
            >
              <Settings className="size-4 text-muted-foreground" strokeWidth={1.75} />
              Profil sozlamalari
            </Link>
            <button
              type="button"
              role="menuitem"
              onClick={onLogout}
              className="flex w-full items-center gap-2.5 rounded-md px-2.5 py-2 text-left text-sm transition-colors hover:bg-muted"
            >
              <LogOut className="size-4 text-muted-foreground" strokeWidth={1.75} />
              Chiqish
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
