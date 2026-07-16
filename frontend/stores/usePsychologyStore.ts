import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  MonthlyReport,
  PsychologistDashboard,
  PsychologistSignalDetail,
  PsychologistSignalListItem,
  ResolveSignalRequest,
  SignalSeverity,
  WatchlistStudent,
} from "@/types/psychologist";

interface PsychologyState {
  dashboard: PsychologistDashboard | null;
  signals: PsychologistSignalListItem[];
  selectedSignal: PsychologistSignalDetail | null;
  watchlist: WatchlistStudent[];
  monthlyReport: MonthlyReport | null;

  fetchDashboard: () => Promise<void>;
  fetchSignals: (filters?: { severity?: SignalSeverity; resolved?: boolean }) => Promise<void>;
  fetchSignalDetail: (signalId: string) => Promise<void>;
  resolveSignal: (signalId: string, request: ResolveSignalRequest) => Promise<void>;
  markManipulation: (signalId: string) => Promise<void>;
  fetchWatchlist: () => Promise<void>;
  addToWatchlist: (studentId: string, reason: string) => Promise<void>;
  removeFromWatchlist: (studentId: string) => Promise<void>;
  fetchMonthlyReport: () => Promise<void>;
}

// One store for the psychologist area — dashboard/signals/watchlist/reports are one cohesive
// UI section (app/dashboard/psychologist/*), matching useAdminStore/useTeacherStore's shape.
export const usePsychologyStore = create<PsychologyState>((set, get) => ({
  dashboard: null,
  signals: [],
  selectedSignal: null,
  watchlist: [],
  monthlyReport: null,

  fetchDashboard: async () => {
    const { data } = await apiClient.get<PsychologistDashboard>("/psychologist/dashboard");
    set({ dashboard: data });
  },

  fetchSignals: async (filters) => {
    const { data } = await apiClient.get<PsychologistSignalListItem[]>("/psychologist/signals", {
      params: filters,
    });
    set({ signals: data });
  },

  fetchSignalDetail: async (signalId) => {
    const { data } = await apiClient.get<PsychologistSignalDetail>(
      `/psychologist/signals/${signalId}`,
    );
    set({ selectedSignal: data });
  },

  resolveSignal: async (signalId, request) => {
    await apiClient.put(`/psychologist/signals/${signalId}/resolve`, request);
    await get().fetchSignalDetail(signalId);
  },

  markManipulation: async (signalId) => {
    await apiClient.put(`/psychologist/signals/${signalId}/mark-manipulation`);
    await get().fetchSignalDetail(signalId);
  },

  fetchWatchlist: async () => {
    const { data } = await apiClient.get<WatchlistStudent[]>("/psychologist/watchlist");
    set({ watchlist: data });
  },

  addToWatchlist: async (studentId, reason) => {
    await apiClient.post(`/psychologist/watchlist/${studentId}`, { reason });
    await get().fetchWatchlist();
  },

  removeFromWatchlist: async (studentId) => {
    await apiClient.delete(`/psychologist/watchlist/${studentId}`);
    await get().fetchWatchlist();
  },

  fetchMonthlyReport: async () => {
    const { data } = await apiClient.get<MonthlyReport>("/psychologist/reports", {
      params: { period: "monthly" },
    });
    set({ monthlyReport: data });
  },
}));
