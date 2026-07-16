import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { AiChatHistoryItem, AiChatRequest, AiChatResponse, SubjectType } from "@/types/aiChat";

interface AiChatState {
  history: AiChatHistoryItem[];
  isSending: boolean;
  fetchHistory: (subject?: SubjectType) => Promise<void>;
  sendMessage: (request: AiChatRequest) => Promise<AiChatResponse>;
}

export const useAiChatStore = create<AiChatState>((set, get) => ({
  history: [],
  isSending: false,

  fetchHistory: async (subject) => {
    const { data } = await apiClient.get<AiChatHistoryItem[]>("/student/ai-chat/history", {
      params: subject ? { subject } : undefined,
    });
    set({ history: data });
  },

  sendMessage: async (request) => {
    set({ isSending: true });
    try {
      const { data } = await apiClient.post<AiChatResponse>("/student/ai-chat", request);
      await get().fetchHistory();
      return data;
    } finally {
      set({ isSending: false });
    }
  },
}));
