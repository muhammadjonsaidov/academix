import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  CreateHomeworkRequest,
  CriteriaItem,
  GenerateLessonPlanRequest,
  GradeSubmissionRequest,
  Homework,
  LessonPlan,
  Syllabus,
  TeacherClass,
  TeacherSubject,
  TeacherSubmissionDetail,
  TeacherSubmissionListItem,
  UniqueTask,
  UpdateLessonPlanRequest,
} from "@/types/teacher";

interface TeacherState {
  classes: TeacherClass[];
  subjects: TeacherSubject[];
  homework: Homework[];
  submissions: TeacherSubmissionListItem[];
  selectedSubmission: TeacherSubmissionDetail | null;
  syllabuses: Syllabus[];
  lessonPlans: LessonPlan[];
  gradingCriteria: CriteriaItem[];
  uniqueTasks: UniqueTask[];

  fetchClasses: () => Promise<void>;
  fetchSubjects: () => Promise<void>;
  fetchHomework: (filters?: { classId?: string; subjectId?: string }) => Promise<void>;
  createHomework: (request: CreateHomeworkRequest) => Promise<Homework>;
  fetchSubmissions: (filters?: { assignmentId?: string; classId?: string }) => Promise<void>;
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
}

// One store for the teacher homework area (classes/subjects/assignments/submissions/syllabuses/
// lesson-plans/grading-criteria/unique-tasks) — mirrors useAdminStore's shape: they're one
// cohesive UI section, not independently evolving domains.
export const useTeacherStore = create<TeacherState>((set, get) => ({
  classes: [],
  subjects: [],
  homework: [],
  submissions: [],
  selectedSubmission: null,
  syllabuses: [],
  lessonPlans: [],
  gradingCriteria: [],
  uniqueTasks: [],

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
    const { data } = await apiClient.post<Homework>("/teacher/homework", request);
    await get().fetchHomework();
    return data;
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
}));
