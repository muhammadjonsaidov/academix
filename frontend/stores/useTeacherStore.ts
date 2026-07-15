import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  CreateHomeworkRequest,
  GradeSubmissionRequest,
  Homework,
  TeacherClass,
  TeacherSubject,
  TeacherSubmissionDetail,
  TeacherSubmissionListItem,
} from "@/types/teacher";

interface TeacherState {
  classes: TeacherClass[];
  subjects: TeacherSubject[];
  homework: Homework[];
  submissions: TeacherSubmissionListItem[];
  selectedSubmission: TeacherSubmissionDetail | null;

  fetchClasses: () => Promise<void>;
  fetchSubjects: () => Promise<void>;
  fetchHomework: (filters?: { classId?: string; subjectId?: string }) => Promise<void>;
  createHomework: (request: CreateHomeworkRequest) => Promise<void>;
  fetchSubmissions: (filters?: { assignmentId?: string; classId?: string }) => Promise<void>;
  fetchSubmission: (submissionId: string) => Promise<void>;
  gradeSubmission: (submissionId: string, request: GradeSubmissionRequest) => Promise<void>;
}

// One store for the teacher homework area (classes/subjects/assignments/submissions) — mirrors
// useAdminStore's shape: they're one cohesive UI section, not independently evolving domains.
export const useTeacherStore = create<TeacherState>((set, get) => ({
  classes: [],
  subjects: [],
  homework: [],
  submissions: [],
  selectedSubmission: null,

  fetchClasses: async () => {
    const { data } = await apiClient.get<TeacherClass[]>("/teacher/classes");
    set({ classes: data });
  },

  fetchSubjects: async () => {
    const { data } = await apiClient.get<TeacherSubject[]>("/teacher/subjects");
    set({ subjects: data });
  },

  fetchHomework: async (filters) => {
    const { data } = await apiClient.get<Homework[]>("/teacher/homework", { params: filters });
    set({ homework: data });
  },

  createHomework: async (request) => {
    await apiClient.post("/teacher/homework", request);
    await get().fetchHomework();
  },

  fetchSubmissions: async (filters) => {
    const { data } = await apiClient.get<TeacherSubmissionListItem[]>("/teacher/submissions", {
      params: filters,
    });
    set({ submissions: data });
  },

  fetchSubmission: async (submissionId) => {
    const { data } = await apiClient.get<TeacherSubmissionDetail>(
      `/teacher/submissions/${submissionId}`,
    );
    set({ selectedSubmission: data });
  },

  gradeSubmission: async (submissionId, request) => {
    await apiClient.post(`/teacher/submissions/${submissionId}/grade`, request);
    await get().fetchSubmission(submissionId);
  },
}));
