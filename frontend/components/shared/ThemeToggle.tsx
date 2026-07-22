"use client";

import { useSyncExternalStore } from "react";
import { Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";

const STORAGE_KEY = "academix-theme";
const CHANGE_EVENT = "academix-theme-change";

function subscribe(callback: () => void) {
  window.addEventListener(CHANGE_EVENT, callback);
  window.addEventListener("storage", callback);
  return () => {
    window.removeEventListener(CHANGE_EVENT, callback);
    window.removeEventListener("storage", callback);
  };
}

function getSnapshot() {
  return document.documentElement.classList.contains("dark");
}

// Server render can't know the stored theme — light is the neutral default; the pre-paint
// script in layout.tsx has already applied the real class by the time hydration reads this.
function getServerSnapshot() {
  return false;
}

/**
 * Light/dark toggle for the "night notebook" palette (globals.css `.dark` block — maintained
 * since the design system landed, wired to an actual control only now). Theme choice persists
 * in localStorage (a UI preference, not a credential — the frontend TDD's "no localStorage"
 * rule is about tokens). layout.tsx applies the saved theme before first paint so there's no
 * light-flash on reload. useSyncExternalStore keeps the icon in sync with the <html> class
 * without a setState-in-effect.
 */
export function ThemeToggle() {
  const isDark = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  function toggle() {
    const next = !document.documentElement.classList.contains("dark");
    document.documentElement.classList.toggle("dark", next);
    try {
      localStorage.setItem(STORAGE_KEY, next ? "dark" : "light");
    } catch {
      // Storage unavailable (private mode etc.) — theme still applies for this page view.
    }
    window.dispatchEvent(new Event(CHANGE_EVENT));
  }

  return (
    <Button
      variant="outline"
      size="icon"
      aria-label={isDark ? "Kunduzgi rejim" : "Tungi rejim"}
      onClick={toggle}
      suppressHydrationWarning
    >
      {isDark ? (
        <Sun className="size-4" strokeWidth={1.75} />
      ) : (
        <Moon className="size-4" strokeWidth={1.75} />
      )}
    </Button>
  );
}
