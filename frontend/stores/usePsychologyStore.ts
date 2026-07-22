import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type {
  MonthlyReport,
  PsychologistDashboard,
  PsychologistSignalDetail,
  PsychologistSignalListItem,
  ResolveSignalRequest,
  SignalSeverity,
  WatchlistStudent,
} from "@/types/psychologist";

interface SignalFilters {
  severity?: SignalSeverity;
  resolved?: boolean;
}

interface PsychologyState {
  dashboard: PsychologistDashboard | null;
  signals: PsychologistSignalListItem[];
  signalsPage: number;
  signalsPageSize: number;
  signalsTotalItems: number;
  /** Last filters passed to fetchSignals — reused by the page/size setters. */
  signalsFilters: SignalFilters;
  selectedSignal: PsychologistSignalDetail | null;
  watchlist: WatchlistStudent[];
  monthlyReport: MonthlyReport | null;

  fetchDashboard: () => Promise<void>;
  fetchSignals: (filters?: SignalFilters) => Promise<void>;
  setSignalsPage: (page: number) => Promise<void>;
  setSignalsPageSize: (size: number) => Promise<void>;
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
  signalsPage: 0,
  signalsPageSize: 20,
  signalsTotalItems: 0,
  signalsFilters: {},
  selectedSignal: null,
  watchlist: [],
  monthlyReport: null,

  fetchDashboard: async () => {
    const { data } = await apiClient.get<PsychologistDashboard>("/psychologist/dashboard");
    set({ dashboard: data });
  },

  // New filters always restart from page 0; page/size setters below reuse the last filters.
  fetchSignals: async (filters) => {
    const { data } = await apiClient.get<PageResponse<PsychologistSignalListItem>>(
      "/psychologist/signals",
      { params: { ...(filters ?? {}), page: 0, size: get().signalsPageSize } },
    );
    set({
      signals: data.items,
      signalsPage: data.page,
      signalsPageSize: data.size,
      signalsTotalItems: data.totalItems,
      signalsFilters: filters ?? {},
    });
  },

  setSignalsPage: async (page) => {
    const { signalsFilters, signalsPageSize } = get();
    const { data } = await apiClient.get<PageResponse<PsychologistSignalListItem>>(
      "/psychologist/signals",
      { params: { ...signalsFilters, page, size: signalsPageSize } },
    );
    set({
      signals: data.items,
      signalsPage: data.page,
      signalsTotalItems: data.totalItems,
    });
  },

  setSignalsPageSize: async (size) => {
    const { data } = await apiClient.get<PageResponse<PsychologistSignalListItem>>(
      "/psychologist/signals",
      { params: { ...get().signalsFilters, page: 0, size } },
    );
    set({
      signals: data.items,
      signalsPage: data.page,
      signalsPageSize: data.size,
      signalsTotalItems: data.totalItems,
    });
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
