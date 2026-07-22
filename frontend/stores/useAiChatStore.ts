import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { AiChatHistoryItem, AiChatRequest, AiChatResponse, SubjectType } from "@/types/aiChat";

interface AiChatState {
  history: AiChatHistoryItem[];
  /** The just-sent, not-yet-answered user message — rendered instantly (optimistic) with a
   *  typing indicator until the model answers. */
  pendingMessage: string | null;
  isSending: boolean;
  fetchHistory: (subject?: SubjectType) => Promise<void>;
  sendMessage: (request: AiChatRequest) => Promise<AiChatResponse>;
}

export const useAiChatStore = create<AiChatState>((set, get) => ({
  history: [],
  pendingMessage: null,
  isSending: false,

  fetchHistory: async (subject) => {
    const { data } = await apiClient.get<AiChatHistoryItem[]>("/student/ai-chat/history", {
      params: subject ? { subject } : undefined,
    });
    set({ history: data });
  },

  // Optimistic: the user's message shows up immediately; the answer replaces the typing
  // indicator when the (10s-ish) Qwen call returns. On failure the pending bubble is dropped
  // and the caller shows the error.
  sendMessage: async (request) => {
    set({ isSending: true, pendingMessage: request.message });
    try {
      const { data } = await apiClient.post<AiChatResponse>("/student/ai-chat", request);
      await get().fetchHistory();
      return data;
    } finally {
      set({ isSending: false, pendingMessage: null });
    }
  },
}));
