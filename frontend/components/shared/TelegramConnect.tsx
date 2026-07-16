"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { useTelegramStore } from "@/stores/useTelegramStore";

// Deviation — academix_tz.md §2.7 gives the endpoints but no UI reference (unlike
// BiometricConsentBanner's §6.6 spec component). Shared across every role since the
// Telegram sub-resource is "barcha rollar uchun umumiy" (common to all roles).
export function TelegramConnect() {
  const status = useTelegramStore((state) => state.status);
  const pendingLink = useTelegramStore((state) => state.pendingLink);
  const isLoading = useTelegramStore((state) => state.isLoading);
  const fetchStatus = useTelegramStore((state) => state.fetchStatus);
  const generateLinkToken = useTelegramStore((state) => state.generateLinkToken);
  const unlink = useTelegramStore((state) => state.unlink);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  if (!status) {
    return null;
  }

  if (status.connected) {
    return (
      <div className="flex items-center gap-3 rounded-lg border border-border p-3 text-sm">
        <span className="text-muted-foreground">
          Telegram ulangan{status.telegramUsername ? `: @${status.telegramUsername}` : ""}
        </span>
        <Button variant="outline" size="sm" onClick={unlink}>
          Uzish
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2 rounded-lg border border-border p-3 text-sm">
      {pendingLink ? (
        <>
          <p className="text-muted-foreground">
            Havolani Telegram&apos;da oching va botni ishga tushiring (5 daqiqa amal qiladi):
          </p>
          <a
            href={pendingLink.linkUrl}
            target="_blank"
            rel="noreferrer"
            className="break-all text-primary underline"
          >
            {pendingLink.linkUrl}
          </a>
          <Button variant="outline" size="sm" onClick={fetchStatus}>
            Ulanganini tekshirish
          </Button>
        </>
      ) : (
        <>
          <p className="text-muted-foreground">
            Bildirishnomalarni Telegram orqali olish uchun ulaning.
          </p>
          <Button size="sm" onClick={generateLinkToken} disabled={isLoading}>
            Telegram&apos;ni ulash
          </Button>
        </>
      )}
    </div>
  );
}
