import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  StudentHomework,
  StudentSubmissionDetail,
  StudentSubmissionListItem,
  SubmissionType,
  SubmitHomeworkResponse,
} from "@/types/student";

interface StudentState {
  homework: StudentHomework[];
  submissions: StudentSubmissionListItem[];
  selectedSubmission: StudentSubmissionDetail | null;

  fetchHomework: () => Promise<void>;
  submitHomework: (
    assignmentId: string,
    type: SubmissionType,
    textContent: string,
    image: File | null,
  ) => Promise<SubmitHomeworkResponse>;
  fetchSubmissions: () => Promise<void>;
  fetchSubmission: (submissionId: string) => Promise<void>;
}

// One store for the student homework area (assignments + own submissions) — mirrors
// useAdminStore/useTeacherStore's shape: one cohesive UI section, not independent domains.
export const useStudentStore = create<StudentState>((set, get) => ({
  homework: [],
  submissions: [],
  selectedSubmission: null,

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
}));
