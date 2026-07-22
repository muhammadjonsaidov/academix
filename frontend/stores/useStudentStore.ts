import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type {
  Badge,
  StudentDashboard,
  StudentHomework,
  StudentProgress,
  StudentSubmissionDetail,
  StudentSubmissionListItem,
  SubmissionType,
  SubmitHomeworkResponse,
  XpHistoryItem,
} from "@/types/student";

interface StudentState {
  homework: StudentHomework[];
  submissions: StudentSubmissionListItem[];
  selectedSubmission: StudentSubmissionDetail | null;
  dashboard: StudentDashboard | null;
  badges: Badge[];
  xpHistory: XpHistoryItem[];
  xpHistoryPage: number;
  xpHistoryPageSize: number;
  xpHistoryTotalItems: number;
  progress: StudentProgress | null;

  fetchHomework: () => Promise<void>;
  submitHomework: (
    assignmentId: string,
    type: SubmissionType,
    textContent: string,
    image: File | null,
  ) => Promise<SubmitHomeworkResponse>;
  fetchSubmissions: () => Promise<void>;
  fetchSubmission: (submissionId: string) => Promise<void>;
  fetchDashboard: () => Promise<void>;
  fetchBadges: () => Promise<void>;
  fetchXpHistory: () => Promise<void>;
  setXpHistoryPage: (page: number) => Promise<void>;
  setXpHistoryPageSize: (size: number) => Promise<void>;
  fetchProgress: () => Promise<void>;
}

// One store for the student homework area (assignments + own submissions + gamification) —
// mirrors useAdminStore/useTeacherStore's shape: one cohesive UI section, not independent domains.
export const useStudentStore = create<StudentState>((set, get) => ({
  homework: [],
  submissions: [],
  selectedSubmission: null,
  dashboard: null,
  badges: [],
  xpHistory: [],
  xpHistoryPage: 0,
  xpHistoryPageSize: 20,
  xpHistoryTotalItems: 0,
  progress: null,

  fetchHomework: async () => {
    const { data } = await apiClient.get<StudentHomework[]>("/student/homework");
    set({ homework: data });
  },

  submitHomework: async (assignmentId, type, textContent, image) => {
    const formData = new FormData();
    formData.append("type", type);
    if (textContent) {
      formData.append("textContent", textContent);
    }
    if (image) {
      formData.append("image", image);
    }
    const { data } = await apiClient.post<SubmitHomeworkResponse>(
      `/student/homework/${assignmentId}/submit`,
      formData,
      { headers: { "Content-Type": "multipart/form-data" } },
    );
    await get().fetchHomework();
    return data;
  },

  fetchSubmissions: async () => {
    const { data } = await apiClient.get<StudentSubmissionListItem[]>("/student/submissions");
    set({ submissions: data });
  },

  fetchSubmission: async (submissionId) => {
    const { data } = await apiClient.get<StudentSubmissionDetail>(
      `/student/submissions/${submissionId}`,
    );
    set({ selectedSubmission: data });
  },

  fetchDashboard: async () => {
    const { data } = await apiClient.get<StudentDashboard>("/student/dashboard");
    set({ dashboard: data });
  },

  fetchBadges: async () => {
    const { data } = await apiClient.get<Badge[]>("/student/badges");
    set({ badges: data });
  },

  fetchXpHistory: async () => {
    const { data } = await apiClient.get<PageResponse<XpHistoryItem>>("/student/xp-history", {
      params: { page: 0, size: get().xpHistoryPageSize },
    });
    set({
      xpHistory: data.items,
      xpHistoryPage: data.page,
      xpHistoryPageSize: data.size,
      xpHistoryTotalItems: data.totalItems,
    });
  },

  setXpHistoryPage: async (page) => {
    const { data } = await apiClient.get<PageResponse<XpHistoryItem>>("/student/xp-history", {
      params: { page, size: get().xpHistoryPageSize },
    });
    set({
      xpHistory: data.items,
      xpHistoryPage: data.page,
      xpHistoryTotalItems: data.totalItems,
    });
  },

  setXpHistoryPageSize: async (size) => {
    const { data } = await apiClient.get<PageResponse<XpHistoryItem>>("/student/xp-history", {
      params: { page: 0, size },
    });
    set({
      xpHistory: data.items,
      xpHistoryPage: data.page,
      xpHistoryPageSize: data.size,
      xpHistoryTotalItems: data.totalItems,
    });
  },

  fetchProgress: async () => {
    const { data } = await apiClient.get<StudentProgress>("/student/progress");
    set({ progress: data });
  },
}));
