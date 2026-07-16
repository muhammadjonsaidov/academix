"use client";

import { useEffect, useRef } from "react";
import { Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useNotificationStore } from "@/stores/useNotificationStore";

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

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <div ref={containerRef} className="relative">
      <Button variant="outline" size="icon" onClick={toggleOpen} aria-label="Bildirishnomalar">
        <Bell className="size-4" />
        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 flex size-4 items-center justify-center rounded-full bg-destructive text-[10px] text-destructive-foreground">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </Button>
      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-80 max-h-96 overflow-y-auto rounded-lg border border-border bg-background shadow-lg">
          {notifications.length === 0 ? (
            <p className="p-4 text-sm text-muted-foreground">Bildirishnomalar yo&apos;q</p>
          ) : (
            <ul className="divide-y divide-border">
              {notifications.map((n) => (
                <li
                  key={n.id}
                  className={`p-3 text-sm ${n.isRead ? "" : "bg-muted/50"}`}
                  onClick={() => !n.isRead && markRead(n.id)}
                >
                  <p className="font-medium">{n.title}</p>
                  {n.body && <p className="text-muted-foreground">{n.body}</p>}
                  <p className="mt-1 text-xs text-muted-foreground">
                    {new Date(n.createdAt).toLocaleString()}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
