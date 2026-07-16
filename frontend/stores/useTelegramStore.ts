import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { TelegramConnectionStatus, TelegramLinkToken } from "@/types/telegram";

interface TelegramState {
  status: TelegramConnectionStatus | null;
  pendingLink: TelegramLinkToken | null;
  isLoading: boolean;
  fetchStatus: () => Promise<void>;
  generateLinkToken: () => Promise<void>;
  unlink: () => Promise<void>;
}

// No push channel exists (frontend_tdd.md — no WebSocket/SSE in the doc), so a connection made
// via the deep-link flow only becomes visible here once the user re-fetches status themselves —
// consistent with the rest of this codebase's "explicit store actions, not pushed" convention.
export const useTelegramStore = create<TelegramState>((set) => ({
  status: null,
  pendingLink: null,
  isLoading: false,

  fetchStatus: async () => {
    const { data } = await apiClient.get<TelegramConnectionStatus>("/notifications/telegram/status");
    set({ status: data });
  },

  generateLinkToken: async () => {
    set({ isLoading: true });
    try {
      const { data } = await apiClient.post<TelegramLinkToken>("/notifications/telegram/link-token");
      set({ pendingLink: data });
    } finally {
      set({ isLoading: false });
    }
  },

  unlink: async () => {
    await apiClient.delete("/notifications/telegram/unlink");
    set({ status: { connected: false, telegramUsername: null }, pendingLink: null });
  },
}));
