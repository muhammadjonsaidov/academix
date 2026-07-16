import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  TeacherClassAnalytics,
  TeacherDashboard,
  TeacherStudentProgress,
} from "@/types/teacherAnalytics";

interface TeacherAnalyticsState {
  dashboard: TeacherDashboard | null;
  studentProgress: TeacherStudentProgress | null;
  classAnalytics: TeacherClassAnalytics | null;

  fetchDashboard: () => Promise<void>;
  fetchStudentProgress: (studentId: string) => Promise<void>;
  fetchClassAnalytics: (classId: string) => Promise<void>;
}

export const useTeacherAnalyticsStore = create<TeacherAnalyticsState>((set) => ({
  dashboard: null,
  studentProgress: null,
  classAnalytics: null,

  fetchDashboard: async () => {
    const { data } = await apiClient.get<TeacherDashboard>("/teacher/dashboard");
    set({ dashboard: data });
  },

  fetchStudentProgress: async (studentId) => {
    const { data } = await apiClient.get<TeacherStudentProgress>(
      `/teacher/students/${studentId}/progress`,
    );
    set({ studentProgress: data });
  },

  fetchClassAnalytics: async (classId) => {
    const { data } = await apiClient.get<TeacherClassAnalytics>(
      `/teacher/classes/${classId}/analytics`,
    );
    set({ classAnalytics: data });
  },
}));
