"use client";

import { useEffect } from "react";
import { X } from "lucide-react";
import { SidebarBrand } from "@/components/shell/SidebarBrand";
import { SidebarNavigation } from "@/components/shell/SidebarNavigation";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { Role } from "@/types/auth";

interface MobileNavigationProps {
  role: Role;
  pathname: string;
  open: boolean;
  onClose: () => void;
}

/** Off-canvas navigation with scroll lock and Escape support for small viewports. */
export function MobileNavigation({ role, pathname, open, onClose }: MobileNavigationProps) {
  useEffect(() => {
    if (!open) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
    };
  }, [onClose, open]);

  return (
    <div
      className={cn("fixed inset-0 z-50 lg:hidden", open ? "" : "pointer-events-none")}
      aria-hidden={!open}
    >
      <button
        type="button"
        tabIndex={open ? 0 : -1}
        aria-label="Menyuni yopish"
        onClick={onClose}
        className={cn(
          "absolute inset-0 bg-foreground/40 backdrop-blur-sm transition-opacity duration-200",
          open ? "opacity-100" : "opacity-0",
        )}
      />
      <aside
        role="dialog"
        aria-modal={open}
        aria-label="Asosiy menyu"
        className={cn(
          "relative z-10 flex h-full w-64 flex-col bg-sidebar shadow-xl transition-transform duration-200 ease-out",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex items-center justify-between border-b border-sidebar-border px-4 py-4">
          <SidebarBrand role={role} compact onNavigate={onClose} />
          <Button variant="ghost" size="icon" aria-label="Menyuni yopish" onClick={onClose}>
            <X className="size-4" strokeWidth={1.75} />
          </Button>
        </div>
        <SidebarNavigation role={role} pathname={pathname} onNavigate={onClose} />
      </aside>
    </div>
  );
}
