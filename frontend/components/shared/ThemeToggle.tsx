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
    const root = document.documentElement;
    const next = !root.classList.contains("dark");
    // Brief whole-page cross-fade around the class flip (see .theme-transition in
    // globals.css — color properties only). setTimeout in an event handler, not an
    // effect. prefers-reduced-motion is handled in CSS: the global reduced-motion
    // rule's !important duration zeroes the transition out.
    root.classList.add("theme-transition");
    window.setTimeout(() => root.classList.remove("theme-transition"), 400);
    root.classList.toggle("dark", next);
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
      className="relative"
      aria-label={isDark ? "Kunduzgi rejim" : "Tungi rejim"}
      onClick={toggle}
      suppressHydrationWarning
    >
      {/* Both icons stay mounted; the .dark class drives a small rotate/scale swap
          (transition-transform — killed by prefers-reduced-motion). Same semantics
          as before: the icon shows the theme you'd switch TO (Moon in light mode,
          Sun in dark mode). */}
      <Moon
        className="size-4 rotate-0 scale-100 transition-transform duration-300 dark:rotate-90 dark:scale-0"
        strokeWidth={1.75}
      />
      <Sun
        className="absolute size-4 -rotate-90 scale-0 transition-transform duration-300 dark:rotate-0 dark:scale-100"
        strokeWidth={1.75}
      />
    </Button>
  );
}
