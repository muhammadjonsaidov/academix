import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { Notification, NotificationPreference } from "@/types/notification";

interface NotificationState {
  notifications: Notification[];
  preferences: NotificationPreference[];
  isOpen: boolean;
  fetchNotifications: () => Promise<void>;
  markRead: (id: string) => Promise<void>;
  markAllRead: () => Promise<void>;
  deleteNotification: (id: string) => Promise<void>;
  clearAll: () => Promise<void>;
  fetchPreferences: () => Promise<void>;
  updatePreference: (
    type: NotificationPreference["type"],
    inAppEnabled: boolean,
    telegramEnabled: boolean,
  ) => Promise<void>;
  toggleOpen: () => void;
}

// No push channel (see useTelegramStore) — fetched explicitly, same convention as the rest of
// this codebase's Zustand stores.
export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  preferences: [],
  isOpen: false,

  fetchNotifications: async () => {
    const { data } = await apiClient.get<Notification[]>("/notifications");
    set({ notifications: data });
  },

  markRead: async (id) => {
    await apiClient.put(`/notifications/${id}/read`);
    set({
      notifications: get().notifications.map((n) =>
        n.id === id ? { ...n, isRead: true, readAt: new Date().toISOString() } : n,
      ),
    });
  },

  markAllRead: async () => {
    await apiClient.put("/notifications/read-all");
    const now = new Date().toISOString();
    set({
      notifications: get().notifications.map((n) =>
        n.isRead ? n : { ...n, isRead: true, readAt: now },
      ),
    });
  },

  deleteNotification: async (id) => {
    await apiClient.delete(`/notifications/${id}`);
    set({ notifications: get().notifications.filter((n) => n.id !== id) });
  },

  clearAll: async () => {
    await apiClient.delete("/notifications");
    set({ notifications: [] });
  },

  fetchPreferences: async () => {
    const { data } = await apiClient.get<NotificationPreference[]>("/notifications/preferences");
    set({ preferences: data });
  },

  updatePreference: async (type, inAppEnabled, telegramEnabled) => {
    const { data } = await apiClient.put<NotificationPreference[]>("/notifications/preferences", {
      type,
      inAppEnabled,
      telegramEnabled,
    });
    set({ preferences: data });
  },

  toggleOpen: () => set((state) => ({ isOpen: !state.isOpen })),
}));
