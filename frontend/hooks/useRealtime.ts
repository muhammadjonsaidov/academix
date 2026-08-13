"use client";

import { useEffect } from "react";
import { useAuthStore } from "@/stores/useAuthStore";
import { useNotificationStore } from "@/stores/useNotificationStore";

/** Payload of the backend's `ai.status` SSE event. */
export interface AiStatusPayload {
  submissionId: string;
  type: "HOMEWORK" | "EXAM";
  status: string;
}

type AiStatusHandler = (payload: AiStatusPayload) => void;

// Module-level handler registry — pages subscribe per submission; the one shared
// EventSource fan-outs to all subscribers, so we never open a connection per page.
const aiStatusHandlers = new Set<AiStatusHandler>();

/** Subscribe to live AI-status events for the session. Returns an unsubscribe fn. */
export function subscribeToAiStatus(handler: AiStatusHandler): () => void {
  aiStatusHandlers.add(handler);
  return () => {
    aiStatusHandlers.delete(handler);
  };
}

// Same fallback as lib/api/client.ts — must stay byte-identical, or the SSE stream
// points at a different origin than every API call in a misconfigured deploy.
const DEFAULT_BASE_URL = "http://localhost:8080/api/v1";

/**
 * One long-lived EventSource per authenticated session, mounted in DashboardShell.
 * The backend pushes `notification.created` (live inbox/bell) and `ai.status` (grading
 * progress). The access token travels as a query param because EventSource cannot set
 * Authorization headers — the backend accepts it only on /api/v1/realtime/**.
 *
 * Token-rotation resilience: access tokens live ~15 minutes, and EventSource reconnects
 * with the *same* URL on failure — without intervention the stream would 401 forever
 * once the token expires and live updates would silently die. On an error event we
 * bootstrap a fresh session (the httpOnly refresh cookie survives), which swaps
 * `accessToken` in the store and re-runs this effect against a fresh, valid URL.
 */
export function useRealtimeEvents() {
  const accessToken = useAuthStore((state) => state.accessToken);
  const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? DEFAULT_BASE_URL;

  useEffect(() => {
    if (!accessToken) return;
    const url = `${baseUrl}/realtime/events?token=${encodeURIComponent(accessToken)}`;
    const es = new EventSource(url);

    es.addEventListener("notification.created", (event) => {
      try {
        JSON.parse((event as MessageEvent).data);
      } catch {
        return;
      }
      // Refetch the inbox so the bell's unread count and list stay live.
      useNotificationStore.getState().fetchNotifications().catch(() => {});
    });

    es.addEventListener("ai.status", (event) => {
      let payload: AiStatusPayload;
      try {
        payload = JSON.parse((event as MessageEvent).data) as AiStatusPayload;
      } catch {
        return;
      }
      aiStatusHandlers.forEach((handler) => handler(payload));
    });

    // Error fires on any transport failure AND on auth rejection — including
    // EventSource's own automatic reconnect attempts, which would retry the stale
    // URL. Guard with a flag so a burst of error events triggers one refresh at most;
    // a successful refresh tears this effect down (new token) and reconnects cleanly.
    let refreshAttempted = false;
    es.onerror = () => {
      if (refreshAttempted) return;
      refreshAttempted = true;
      useAuthStore
        .getState()
        .bootstrapSession()
        .then((ok) => {
          if (!ok) {
            // Session genuinely gone — stop retrying and clear the session; the
            // DashboardShell guard redirects to /login.
            es.close();
            useAuthStore.getState().logout();
          }
        });
    };

    return () => es.close();
  }, [accessToken, baseUrl]);
}
