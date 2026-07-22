"use client";

import { useEffect, useRef, useState } from "react";
import { Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useTelegramStore } from "@/stores/useTelegramStore";

// Deviation — academix_tz.md §2.7 gives the endpoints but no UI reference (unlike
// BiometricConsentBanner's §6.6 spec component). Shared across every role. Rendered as
// a compact icon-button + popover matching NotificationBell, so the header stays a
// tidy icon row instead of carrying a full inline call-to-action card.
export function TelegramConnect() {
  const status = useTelegramStore((state) => state.status);
  const pendingLink = useTelegramStore((state) => state.pendingLink);
  const isLoading = useTelegramStore((state) => state.isLoading);
  const fetchStatus = useTelegramStore((state) => state.fetchStatus);
  const generateLinkToken = useTelegramStore((state) => state.generateLinkToken);
  const unlink = useTelegramStore((state) => state.unlink);
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

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

  if (!status) {
    return null;
  }

  return (
    <div ref={containerRef} className="relative">
      <Button
        variant="outline"
        size="icon"
        aria-label={status.connected ? "Telegram ulangan" : "Telegram'ni ulash"}
        aria-expanded={isOpen}
        onClick={() => setIsOpen((open) => !open)}
      >
        <Send className="size-4" strokeWidth={1.75} />
        {status.connected && (
          <span
            className="absolute -top-0.5 -right-0.5 size-2 rounded-full bg-success"
            aria-hidden
          />
        )}
      </Button>
      {isOpen && (
        <div className="absolute right-0 z-50 mt-2 w-72 rounded-lg border border-border bg-background p-4 shadow-lg">
          {status.connected ? (
            <div className="flex flex-col gap-3 text-sm">
              <p className="text-muted-foreground">
                Telegram ulangan{status.telegramUsername ? `: @${status.telegramUsername}` : ""}.
                Bildirishnomalar botga yuboriladi.
              </p>
              <Button variant="outline" size="sm" onClick={unlink}>
                Uzish
              </Button>
            </div>
          ) : pendingLink ? (
            <div className="flex flex-col gap-3 text-sm">
              <p className="text-muted-foreground">
                Havolani Telegram&apos;da oching va botni ishga tushiring (5 daqiqa amal qiladi):
              </p>
              <a
                href={pendingLink.linkUrl}
                target="_blank"
                rel="noreferrer"
                className="break-all text-primary underline underline-offset-4"
              >
                {pendingLink.linkUrl}
              </a>
              <Button variant="outline" size="sm" onClick={fetchStatus}>
                Ulanganini tekshirish
              </Button>
            </div>
          ) : (
            <div className="flex flex-col gap-3 text-sm">
              <p className="text-muted-foreground">
                Bildirishnomalarni Telegram orqali olish uchun hisobingizni botga ulang.
              </p>
              <Button size="sm" onClick={generateLinkToken} disabled={isLoading}>
                Telegram&apos;ni ulash
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
