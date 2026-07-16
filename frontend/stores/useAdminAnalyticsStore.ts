import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  AdminDashboard,
  ClassProgress,
  PeriodProgress,
  TeacherRanking,
} from "@/types/adminAnalytics";

interface AdminAnalyticsState {
  dashboard: AdminDashboard | null;
  classesComparison: ClassProgress[];
  teachersRanking: TeacherRanking[];
  schoolProgress: PeriodProgress[];

  fetchDashboard: () => Promise<void>;
  fetchClassesComparison: (period?: string) => Promise<void>;
  fetchTeachersRanking: () => Promise<void>;
  fetchSchoolProgress: (period?: string) => Promise<void>;
}

// Separate from useAdminStore (classes/teachers/students CRUD) — analytics is a distinct,
// independently-evolving domain, same reasoning as useExamStore vs useAdminStore.
export const useAdminAnalyticsStore = create<AdminAnalyticsState>((set) => ({
  dashboard: null,
  classesComparison: [],
  teachersRanking: [],
  schoolProgress: [],

  fetchDashboard: async () => {
    const { data } = await apiClient.get<AdminDashboard>("/admin/dashboard");
    set({ dashboard: data });
  },

  fetchClassesComparison: async (period) => {
    const { data } = await apiClient.get<ClassProgress[]>("/admin/analytics/classes-comparison", {
      params: { period },
    });
    set({ classesComparison: data });
  },

  fetchTeachersRanking: async () => {
    const { data } = await apiClient.get<TeacherRanking[]>("/admin/analytics/teachers-ranking");
    set({ teachersRanking: data });
  },

  fetchSchoolProgress: async (period) => {
    const { data } = await apiClient.get<PeriodProgress[]>("/admin/analytics/school-progress", {
      params: { period },
    });
    set({ schoolProgress: data });
  },
}));
