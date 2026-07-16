import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  BulkUploadExamSubmissionsResponse,
  CreateExamRequest,
  CreateExamResponse,
  ExamListItem,
  ExamSubmission,
  GradeExamSubmissionRequest,
} from "@/types/teacher";
import type { StudentExamDetail, StudentExamListItem } from "@/types/student";

interface ExamState {
  exams: ExamListItem[];
  examSubmissions: ExamSubmission[];
  studentExams: StudentExamListItem[];
  selectedStudentExam: StudentExamDetail | null;

  fetchExams: (filters?: { classId?: string; subjectId?: string }) => Promise<void>;
  createExam: (request: CreateExamRequest) => Promise<CreateExamResponse>;
  bulkUploadSubmissions: (
    examId: string,
    files: File[],
    studentIds: string[],
  ) => Promise<BulkUploadExamSubmissionsResponse>;
  fetchExamSubmissions: (examId: string) => Promise<void>;
  gradeExamSubmission: (
    examId: string,
    submissionId: string,
    request: GradeExamSubmissionRequest,
  ) => Promise<void>;
  approveAllExamSubmissions: (examId: string) => Promise<void>;

  fetchStudentExams: () => Promise<void>;
  fetchStudentExamDetail: (examId: string) => Promise<void>;
}

// One store for the exam domain — teacher CRUD/grading and student results both live here,
// matching the single useExamStore this project's frontend TDD (§6.3) names, rather than
// splitting by role like the type files do.
export const useExamStore = create<ExamState>((set, get) => ({
  exams: [],
  examSubmissions: [],
  studentExams: [],
  selectedStudentExam: null,

  fetchExams: async (filters) => {
    const { data } = await apiClient.get<ExamListItem[]>("/teacher/exams", { params: filters });
    set({ exams: data });
  },

  createExam: async (request) => {
    const { data } = await apiClient.post<CreateExamResponse>("/teacher/exams", request);
    await get().fetchExams();
    return data;
  },

  bulkUploadSubmissions: async (examId, files, studentIds) => {
    const formData = new FormData();
    files.forEach((file) => formData.append("images", file));
    studentIds.forEach((id) => formData.append("studentIds", id));
    const { data } = await apiClient.post<BulkUploadExamSubmissionsResponse>(
      `/teacher/exams/${examId}/submissions/bulk-upload`,
      formData,
      { headers: { "Content-Type": "multipart/form-data" } },
    );
    return data;
  },

  fetchExamSubmissions: async (examId) => {
    const { data } = await apiClient.get<ExamSubmission[]>(
      `/teacher/exams/${examId}/submissions`,
    );
    set({ examSubmissions: data });
  },

  gradeExamSubmission: async (examId, submissionId, request) => {
    await apiClient.post(
      `/teacher/exams/${examId}/submissions/${submissionId}/grade`,
      request,
    );
    await get().fetchExamSubmissions(examId);
  },

  approveAllExamSubmissions: async (examId) => {
    await apiClient.post(`/teacher/exams/${examId}/submissions/approve-all`);
    await get().fetchExamSubmissions(examId);
  },

  fetchStudentExams: async () => {
    const { data } = await apiClient.get<StudentExamListItem[]>("/student/exams");
    set({ studentExams: data });
  },

  fetchStudentExamDetail: async (examId) => {
    const { data } = await apiClient.get<StudentExamDetail>(`/student/exams/${examId}`);
    set({ selectedStudentExam: data });
  },
}));
