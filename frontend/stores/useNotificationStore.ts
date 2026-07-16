import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { Notification } from "@/types/notification";

interface NotificationState {
  notifications: Notification[];
  isOpen: boolean;
  fetchNotifications: () => Promise<void>;
  markRead: (id: string) => Promise<void>;
  toggleOpen: () => void;
}

// No push channel (see useTelegramStore) — fetched explicitly, same convention as the rest of
// this codebase's Zustand stores.
export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
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

  toggleOpen: () => set((state) => ({ isOpen: !state.isOpen })),
}));
