import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type {
  CreateHomeworkRequest,
  CriteriaItem,
  GradeSubmissionRequest,
  Homework,
  SignalSeverity,
  TeacherClass,
  TeacherPsychologicalSignal,
  TeacherStudent,
  TeacherSubject,
  TeacherSubmissionDetail,
  TeacherSubmissionListItem,
  SubmissionStatus,
  UpdateHomeworkRequest,
} from "@/types/teacher";

interface SubmissionFilters {
  assignmentId?: string;
  classId?: string;
  status?: SubmissionStatus;
}

interface TeacherState {
  classes: TeacherClass[];
  subjects: TeacherSubject[];
  classStudents: TeacherStudent[];
  psychologicalSignals: TeacherPsychologicalSignal[];
  homework: Homework[];
  submissions: TeacherSubmissionListItem[];
  submissionsPage: number;
  submissionsPageSize: number;
  submissionsTotalItems: number;
  /** Last filters passed to fetchSubmissions — reused by the page/size setters. */
  submissionsFilters: SubmissionFilters;
  selectedSubmission: TeacherSubmissionDetail | null;
  gradingCriteria: CriteriaItem[];

  fetchClasses: () => Promise<void>;
  fetchSubjects: () => Promise<void>;
  fetchClassStudents: (classId: string) => Promise<void>;
  fetchPsychologicalSignals: (severity?: SignalSeverity) => Promise<void>;
  resolvePsychologicalSignal: (signalId: string) => Promise<void>;
  fetchHomework: (filters?: { classId?: string; subjectId?: string }) => Promise<void>;
  createHomework: (request: CreateHomeworkRequest) => Promise<Homework>;
  updateHomework: (assignmentId: string, request: UpdateHomeworkRequest) => Promise<Homework>;
  deleteHomework: (assignmentId: string) => Promise<void>;
  fetchSubmissions: (filters?: SubmissionFilters) => Promise<void>;
  setSubmissionsPage: (page: number) => Promise<void>;
  setSubmissionsPageSize: (size: number) => Promise<void>;
  fetchSubmission: (submissionId: string) => Promise<void>;
  gradeSubmission: (submissionId: string, request: GradeSubmissionRequest) => Promise<void>;

  fetchGradingCriteria: (subjectId: string) => Promise<void>;
  updateGradingCriteria: (subjectId: string, criteria: CriteriaItem[]) => Promise<void>;

  resetStudentPassword: (studentId: string) => Promise<string>;
}

// One store for the teacher homework area (classes/subjects/assignments/submissions/
// grading-criteria) — mirrors useAdminStore's shape: they're one
// cohesive UI section, not independently evolving domains.
export const useTeacherStore = create<TeacherState>((set, get) => ({
  classes: [],
  subjects: [],
  homework: [],
  submissions: [],
  submissionsPage: 0,
  submissionsPageSize: 20,
  submissionsTotalItems: 0,
  submissionsFilters: {},
  selectedSubmission: null,
  gradingCriteria: [],
  classStudents: [],
  psychologicalSignals: [],

  fetchClasses: async () => {
    const { data } = await apiClient.get<TeacherClass[]>("/teacher/classes");
    set({ classes: data });
  },

  fetchSubjects: async () => {
    const { data } = await apiClient.get<TeacherSubject[]>("/teacher/subjects");
    set({ subjects: data });
  },

  fetchClassStudents: async (classId) => {
    const { data } = await apiClient.get<TeacherStudent[]>(
      `/teacher/classes/${classId}/students`,
    );
    set({ classStudents: data });
  },

  fetchPsychologicalSignals: async (severity) => {
    const { data } = await apiClient.get<TeacherPsychologicalSignal[]>(
      "/teacher/psychological-signals",
      { params: severity ? { severity } : undefined },
    );
    set({ psychologicalSignals: data });
  },

  resolvePsychologicalSignal: async (signalId) => {
    await apiClient.put(`/teacher/psychological-signals/${signalId}/resolve`);
    await get().fetchPsychologicalSignals();
  },

  fetchHomework: async (filters) => {
    const { data } = await apiClient.get<Homework[]>("/teacher/homework", { params: filters });
    set({ homework: data });
  },

  createHomework: async (request) => {
    const { data } = await apiClient.post<Homework>("/teacher/homework", request);
    await get().fetchHomework();
    return data;
  },

  updateHomework: async (assignmentId, request) => {
    const { data } = await apiClient.put<Homework>(
      `/teacher/homework/${assignmentId}`,
      request,
    );
    await get().fetchHomework();
    return data;
  },

  deleteHomework: async (assignmentId) => {
    await apiClient.delete(`/teacher/homework/${assignmentId}`);
    await get().fetchHomework();
  },

  // New filters always restart from page 0; page/size setters below reuse the last filters.
  fetchSubmissions: async (filters) => {
    const { data } = await apiClient.get<PageResponse<TeacherSubmissionListItem>>(
      "/teacher/submissions",
      { params: { ...(filters ?? {}), page: 0, size: get().submissionsPageSize } },
    );
    set({
      submissions: data.items,
      submissionsPage: data.page,
      submissionsPageSize: data.size,
      submissionsTotalItems: data.totalItems,
      submissionsFilters: filters ?? {},
    });
  },

  setSubmissionsPage: async (page) => {
    const { submissionsFilters, submissionsPageSize } = get();
    const { data } = await apiClient.get<PageResponse<TeacherSubmissionListItem>>(
      "/teacher/submissions",
      { params: { ...submissionsFilters, page, size: submissionsPageSize } },
    );
    set({
      submissions: data.items,
      submissionsPage: data.page,
      submissionsTotalItems: data.totalItems,
    });
  },

  setSubmissionsPageSize: async (size) => {
    const { data } = await apiClient.get<PageResponse<TeacherSubmissionListItem>>(
      "/teacher/submissions",
      { params: { ...get().submissionsFilters, page: 0, size } },
    );
    set({
      submissions: data.items,
      submissionsPage: data.page,
      submissionsPageSize: data.size,
      submissionsTotalItems: data.totalItems,
    });
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

  fetchGradingCriteria: async (subjectId) => {
    const { data } = await apiClient.get<CriteriaItem[]>("/teacher/grading-criteria", {
      params: { subjectId },
    });
    set({ gradingCriteria: data });
  },

  updateGradingCriteria: async (subjectId, criteria) => {
    const { data } = await apiClient.put<CriteriaItem[]>(
      `/teacher/grading-criteria/${subjectId}`,
      { criteria },
    );
    set({ gradingCriteria: data });
  },

  resetStudentPassword: async (studentId) => {
    const { data } = await apiClient.put<{ temporaryPassword: string }>(
      `/teacher/students/${studentId}/reset-password`,
    );
    return data.temporaryPassword;
  },
}));
