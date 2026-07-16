import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  ChildOverview,
  ChildSummary,
  ParentDashboard,
  ParentGrades,
  ParentProgress,
  ParentSubmission,
  StudentHomeworkItem,
} from "@/types/parent";

interface ParentState {
  dashboard: ParentDashboard | null;
  children: ChildSummary[];
  selectedOverview: ChildOverview | null;
  selectedProgress: ParentProgress | null;
  selectedHomework: StudentHomeworkItem[];
  selectedSubmissions: ParentSubmission[];
  selectedGrades: ParentGrades | null;

  fetchDashboard: () => Promise<void>;
  fetchChildren: () => Promise<void>;
  fetchOverview: (studentId: string) => Promise<void>;
  fetchProgress: (studentId: string) => Promise<void>;
  fetchHomework: (studentId: string) => Promise<void>;
  fetchSubmissions: (studentId: string) => Promise<void>;
  fetchGrades: (studentId: string) => Promise<void>;
  downloadSemesterReport: (studentId: string) => Promise<void>;
}

// One store for the parent dashboard area — dashboard/children/overview/progress/homework/
// submissions/grades are one cohesive UI section, matching useAdminStore/useTeacherStore's
// shape. Biometric consent lives in its own useConsentStore (frontend_tdd.md §6.6's named
// store), kept separate since it's cross-cutting (a banner shown outside this section too).
export const useParentStore = create<ParentState>((set) => ({
  dashboard: null,
  children: [],
  selectedOverview: null,
  selectedProgress: null,
  selectedHomework: [],
  selectedSubmissions: [],
  selectedGrades: null,

  fetchDashboard: async () => {
    const { data } = await apiClient.get<ParentDashboard>("/parent/dashboard");
    set({ dashboard: data });
  },

  fetchChildren: async () => {
    const { data } = await apiClient.get<ChildSummary[]>("/parent/children");
    set({ children: data });
  },

  fetchOverview: async (studentId) => {
    const { data } = await apiClient.get<ChildOverview>(
      `/parent/children/${studentId}/overview`,
    );
    set({ selectedOverview: data });
  },

  fetchProgress: async (studentId) => {
    const { data } = await apiClient.get<ParentProgress>(
      `/parent/children/${studentId}/progress`,
    );
    set({ selectedProgress: data });
  },

  fetchHomework: async (studentId) => {
    const { data } = await apiClient.get<StudentHomeworkItem[]>(
      `/parent/children/${studentId}/homework`,
    );
    set({ selectedHomework: data });
  },

  fetchSubmissions: async (studentId) => {
    const { data } = await apiClient.get<ParentSubmission[]>(
      `/parent/children/${studentId}/submissions`,
    );
    set({ selectedSubmissions: data });
  },

  fetchGrades: async (studentId) => {
    const { data } = await apiClient.get<ParentGrades>(`/parent/children/${studentId}/grades`);
    set({ selectedGrades: data });
  },

  // Authenticated blob fetch — a plain <a href> can't carry the Authorization header (same
  // pattern as admin reports' download).
  downloadSemesterReport: async (studentId) => {
    const response = await apiClient.get(`/parent/children/${studentId}/semester-report/download`, {
      responseType: "blob",
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    link.download = `semester-report-${studentId}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
}));
