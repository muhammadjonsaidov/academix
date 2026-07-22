import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type {
  CreateHomeworkRequest,
  CriteriaItem,
  GenerateLessonPlanRequest,
  GradeSubmissionRequest,
  HandwritingResetRequest,
  HandwritingResetResponse,
  Homework,
  LessonPlan,
  Syllabus,
  SignalSeverity,
  TeacherClass,
  TeacherPsychologicalSignal,
  TeacherStudent,
  TeacherSubject,
  TeacherSubmissionDetail,
  TeacherSubmissionListItem,
  SubmissionStatus,
  UniqueTask,
  UpdateHomeworkRequest,
  UpdateLessonPlanRequest,
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
  syllabuses: Syllabus[];
  lessonPlans: LessonPlan[];
  gradingCriteria: CriteriaItem[];
  uniqueTasks: UniqueTask[];

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

  fetchSyllabuses: (filters?: { subjectId?: string; classId?: string }) => Promise<void>;
  uploadSyllabus: (
    file: File,
    subjectId: string,
    classId: string,
    title: string,
  ) => Promise<void>;

  fetchLessonPlans: (filters?: { subjectId?: string; classId?: string }) => Promise<void>;
  generateLessonPlan: (request: GenerateLessonPlanRequest) => Promise<void>;
  updateLessonPlan: (planId: string, request: UpdateLessonPlanRequest) => Promise<void>;

  fetchGradingCriteria: (subjectId: string) => Promise<void>;
  updateGradingCriteria: (subjectId: string, criteria: CriteriaItem[]) => Promise<void>;

  fetchUniqueTasks: (assignmentId: string) => Promise<void>;
  approveUniqueTask: (assignmentId: string, taskId: string) => Promise<void>;
  editUniqueTask: (assignmentId: string, taskId: string, taskContent: string) => Promise<void>;
  approveAllUniqueTasks: (assignmentId: string) => Promise<void>;
  submitHomework: (assignmentId: string) => Promise<void>;

  resetHandwritingProfile: (
    studentId: string,
    request: HandwritingResetRequest,
  ) => Promise<HandwritingResetResponse>;

  resetStudentPassword: (studentId: string) => Promise<string>;
}

// One store for the teacher homework area (classes/subjects/assignments/submissions/syllabuses/
// lesson-plans/grading-criteria/unique-tasks) — mirrors useAdminStore's shape: they're one
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
  syllabuses: [],
  lessonPlans: [],
  gradingCriteria: [],
  uniqueTasks: [],
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

  fetchSyllabuses: async (filters) => {
    const { data } = await apiClient.get<Syllabus[]>("/teacher/syllabuses", { params: filters });
    set({ syllabuses: data });
  },

  uploadSyllabus: async (file, subjectId, classId, title) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("subjectId", subjectId);
    formData.append("classId", classId);
    formData.append("title", title);
    await apiClient.post("/teacher/syllabuses", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    await get().fetchSyllabuses();
  },

  fetchLessonPlans: async (filters) => {
    const { data } = await apiClient.get<LessonPlan[]>("/teacher/lesson-plans", {
      params: filters,
    });
    set({ lessonPlans: data });
  },

  generateLessonPlan: async (request) => {
    await apiClient.post("/teacher/lesson-plans/generate", request);
    await get().fetchLessonPlans();
  },

  updateLessonPlan: async (planId, request) => {
    await apiClient.put(`/teacher/lesson-plans/${planId}`, request);
    await get().fetchLessonPlans();
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

  fetchUniqueTasks: async (assignmentId) => {
    const { data } = await apiClient.get<UniqueTask[]>(
      `/teacher/homework/${assignmentId}/unique-tasks`,
    );
    set({ uniqueTasks: data });
  },

  approveUniqueTask: async (assignmentId, taskId) => {
    await apiClient.put(`/teacher/homework/${assignmentId}/unique-tasks/${taskId}/approve`);
    await get().fetchUniqueTasks(assignmentId);
  },

  editUniqueTask: async (assignmentId, taskId, taskContent) => {
    await apiClient.put(`/teacher/homework/${assignmentId}/unique-tasks/${taskId}`, {
      taskContent,
    });
    await get().fetchUniqueTasks(assignmentId);
  },

  approveAllUniqueTasks: async (assignmentId) => {
    await apiClient.post(`/teacher/homework/${assignmentId}/unique-tasks/approve-all`);
    await get().fetchUniqueTasks(assignmentId);
  },

  submitHomework: async (assignmentId) => {
    await apiClient.post(`/teacher/homework/${assignmentId}/submit`);
    await get().fetchHomework();
  },

  resetHandwritingProfile: async (studentId, request) => {
    const { data } = await apiClient.put<HandwritingResetResponse>(
      `/teacher/students/${studentId}/handwriting/reset`,
      request,
    );
    return data;
  },

  resetStudentPassword: async (studentId) => {
    const { data } = await apiClient.put<{ temporaryPassword: string }>(
      `/teacher/students/${studentId}/reset-password`,
    );
    return data.temporaryPassword;
  },
}));
