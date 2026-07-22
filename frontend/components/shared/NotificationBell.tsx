"use client";

import { useEffect, useRef } from "react";
import {
  Award,
  Bell,
  BookOpen,
  Bot,
  CheckCheck,
  ClipboardCheck,
  Clock,
  Fingerprint,
  Flame,
  HeartPulse,
  LineChart,
  type LucideIcon,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { useNotificationStore } from "@/stores/useNotificationStore";
import type { NotificationType } from "@/types/notification";

// Per-type icon + accent so a glance separates "yutuq" from "signal" from "muddat".
// Colors reuse the system's semantic tokens only — no new hues.
const TYPE_META: Record<NotificationType, { icon: LucideIcon; className: string }> = {
  HOMEWORK_ASSIGNED: { icon: BookOpen, className: "text-ink" },
  DEADLINE_REMINDER: { icon: Clock, className: "text-severity-medium" },
  HOMEWORK_GRADED: { icon: ClipboardCheck, className: "text-success" },
  STREAK_BROKEN: { icon: Flame, className: "text-status-skipped" },
  STREAK_MILESTONE: { icon: Flame, className: "text-role-student" },
  BADGE_EARNED: { icon: Award, className: "text-role-student" },
  PSYCHOLOGICAL_ALERT: { icon: HeartPulse, className: "text-severity-high" },
  LATE_SUBMISSION: { icon: Clock, className: "text-severity-medium" },
  CLASS_PROGRESS_REPORT: { icon: LineChart, className: "text-ink" },
  HANDWRITING_PROFILE_RESET: { icon: Fingerprint, className: "text-ink" },
  AI_BUDGET_LOW: { icon: Bot, className: "text-severity-medium" },
};

function relativeTime(iso: string): string {
  const then = new Date(iso).getTime();
  const minutes = Math.floor((Date.now() - then) / 60_000);
  if (minutes < 1) return "hozirgina";
  if (minutes < 60) return `${minutes} daqiqa oldin`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} soat oldin`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "kecha";
  if (days < 7) return `${days} kun oldin`;
  return new Date(iso).toLocaleDateString();
}

// Deviation — no notification inbox UI is documented anywhere (matches the backend
// NotificationController's own "deviation" flag). Shared across every role.
export function NotificationBell() {
  const notifications = useNotificationStore((state) => state.notifications);
  const isOpen = useNotificationStore((state) => state.isOpen);
  const fetchNotifications = useNotificationStore((state) => state.fetchNotifications);
  const markRead = useNotificationStore((state) => state.markRead);
  const toggleOpen = useNotificationStore((state) => state.toggleOpen);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        if (isOpen) toggleOpen();
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen, toggleOpen]);

  const unread = notifications.filter((n) => !n.isRead);

  function markAllRead() {
    unread.forEach((n) => markRead(n.id));
  }

  return (
    <div ref={containerRef} className="relative">
      <Button
        variant="outline"
        size="icon"
        onClick={toggleOpen}
        aria-label="Bildirishnomalar"
        aria-expanded={isOpen}
      >
        <Bell className="size-4" strokeWidth={1.75} />
        {unread.length > 0 && (
          <span className="absolute -top-1 -right-1 flex size-4 items-center justify-center rounded-full bg-pen-red text-[10px] font-semibold text-severity-critical-foreground">
            {unread.length > 9 ? "9+" : unread.length}
          </span>
        )}
      </Button>
      {isOpen && (
        <div className="animate-rise absolute right-0 z-50 mt-2 flex max-h-[26rem] w-88 flex-col overflow-hidden rounded-lg border border-border bg-card shadow-lg">
          <div className="flex items-center justify-between gap-2 border-b border-border px-4 py-2.5">
            <p className="text-sm font-semibold">
              Bildirishnomalar
              {unread.length > 0 && (
                <span className="text-muted-foreground ml-1.5 font-normal">
                  ({unread.length} yangi)
                </span>
              )}
            </p>
            {unread.length > 0 && (
              <Button variant="ghost" size="xs" onClick={markAllRead}>
                <CheckCheck data-icon="inline-start" className="size-3.5" strokeWidth={1.75} />
                Barchasini o&apos;qish
              </Button>
            )}
          </div>
          {notifications.length === 0 ? (
            <div className="flex flex-col items-center gap-2 px-4 py-10 text-center">
              <Bell className="size-5 text-muted-foreground" strokeWidth={1.5} />
              <p className="text-sm text-muted-foreground">Hozircha bildirishnomalar yo&apos;q.</p>
            </div>
          ) : (
            <ul className="divide-y divide-border overflow-y-auto">
              {notifications.map((n) => {
                const meta = TYPE_META[n.type] ?? { icon: Bell, className: "text-ink" };
                const Icon = meta.icon;
                return (
                  <li key={n.id}>
                    <button
                      type="button"
                      onClick={() => !n.isRead && markRead(n.id)}
                      className={cn(
                        "flex w-full items-start gap-3 px-4 py-3 text-left text-sm transition-colors",
                        n.isRead ? "opacity-70" : "bg-accent/40 hover:bg-accent/60",
                      )}
                    >
                      <Icon
                        className={cn("mt-0.5 size-4 shrink-0", meta.className)}
                        strokeWidth={1.75}
                      />
                      <span className="flex min-w-0 flex-col gap-0.5">
                        <span className="flex items-center gap-1.5 font-medium">
                          <span className="truncate">{n.title}</span>
                          {!n.isRead && (
                            <span className="size-1.5 shrink-0 rounded-full bg-pen-red" aria-hidden />
                          )}
                        </span>
                        {n.body && (
                          <span className="line-clamp-2 text-muted-foreground">{n.body}</span>
                        )}
                        <span className="font-data text-xs text-muted-foreground">
                          {relativeTime(n.createdAt)}
                        </span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
