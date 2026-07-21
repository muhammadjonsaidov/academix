import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  AdminDataDeletionRequest,
  Assignment,
  CreateAssignmentRequest,
  CreateClassRequest,
  CreateStudentRequest,
  InvitePsychologistRequest,
  InviteTeacherRequest,
  LinkParentRequest,
  ParentLink,
  Psychologist,
  School,
  SchoolClass,
  Student,
  Teacher,
  UpdateClassRequest,
  UpdateSchoolRequest,
} from "@/types/admin";

interface AdminState {
  classes: SchoolClass[];
  teachers: Teacher[];
  students: Student[];
  school: School | null;
  psychologists: Psychologist[];
  assignments: Assignment[];
  dataDeletionRequests: AdminDataDeletionRequest[];

  fetchClasses: () => Promise<void>;
  createClass: (request: CreateClassRequest) => Promise<void>;
  updateClass: (classId: string, request: UpdateClassRequest) => Promise<void>;
  deleteClass: (classId: string) => Promise<void>;

  fetchTeachers: () => Promise<void>;
  inviteTeacher: (request: InviteTeacherRequest) => Promise<void>;
  setTeacherActive: (teacherId: string, active: boolean) => Promise<void>;

  fetchStudents: (filters?: { classId?: string; search?: string }) => Promise<void>;
  createStudent: (request: CreateStudentRequest) => Promise<void>;
  transferStudentClass: (studentId: string, newClassId: string) => Promise<void>;
  unlockHandwritingReset: (studentId: string) => Promise<void>;
  linkParentToStudent: (request: LinkParentRequest) => Promise<ParentLink>;

  fetchSchool: () => Promise<void>;
  updateSchool: (request: UpdateSchoolRequest) => Promise<void>;

  fetchPsychologists: () => Promise<void>;
  invitePsychologist: (request: InvitePsychologistRequest) => Promise<void>;
  setPsychologistActive: (psychologistId: string, active: boolean) => Promise<void>;

  fetchAssignments: () => Promise<void>;
  createAssignment: (request: CreateAssignmentRequest) => Promise<void>;
  deleteAssignment: (assignmentId: string) => Promise<void>;

  fetchDataDeletionRequests: () => Promise<void>;
  approveDataDeletionRequest: (id: string) => Promise<void>;
}

// One store for all three admin CRUD areas (classes/teachers/students) — they're a single
// Sprint 1 UI section (app/dashboard/admin/{classes,teachers,students}), not independently
// evolving domains like homework/exams, so splitting into 3 stores would just be indirection.
export const useAdminStore = create<AdminState>((set, get) => ({
  classes: [],
  teachers: [],
  students: [],
  school: null,
  psychologists: [],
  assignments: [],
  dataDeletionRequests: [],

  fetchClasses: async () => {
    const { data } = await apiClient.get<SchoolClass[]>("/admin/classes");
    set({ classes: data });
  },

  createClass: async (request) => {
    await apiClient.post("/admin/classes", request);
    await get().fetchClasses();
  },

  updateClass: async (classId, request) => {
    await apiClient.put(`/admin/classes/${classId}`, request);
    await get().fetchClasses();
  },

  deleteClass: async (classId) => {
    await apiClient.delete(`/admin/classes/${classId}`);
    await get().fetchClasses();
  },

  fetchTeachers: async () => {
    const { data } = await apiClient.get<Teacher[]>("/admin/teachers");
    set({ teachers: data });
  },

  inviteTeacher: async (request) => {
    await apiClient.post("/admin/teachers/invite", request);
    await get().fetchTeachers();
  },

  setTeacherActive: async (teacherId, active) => {
    await apiClient.put(`/admin/teachers/${teacherId}/${active ? "activate" : "deactivate"}`);
    await get().fetchTeachers();
  },

  fetchStudents: async (filters) => {
    const { data } = await apiClient.get<Student[]>("/admin/students", { params: filters });
    set({ students: data });
  },

  createStudent: async (request) => {
    await apiClient.post("/admin/students", request);
    await get().fetchStudents();
  },

  transferStudentClass: async (studentId, newClassId) => {
    await apiClient.put(`/admin/students/${studentId}/transfer-class`, { newClassId });
    await get().fetchStudents();
  },

  unlockHandwritingReset: async (studentId) => {
    await apiClient.put(`/admin/students/${studentId}/handwriting/unlock-reset`);
  },

  linkParentToStudent: async (request) => {
    const { data } = await apiClient.post<ParentLink>("/admin/parents/link", request);
    return data;
  },

  fetchSchool: async () => {
    const { data } = await apiClient.get<School>("/admin/school");
    set({ school: data });
  },

  updateSchool: async (request) => {
    const { data } = await apiClient.put<School>("/admin/school", request);
    set({ school: data });
  },

  fetchPsychologists: async () => {
    const { data } = await apiClient.get<Psychologist[]>("/admin/psychologists");
    set({ psychologists: data });
  },

  invitePsychologist: async (request) => {
    await apiClient.post("/admin/psychologists/invite", request);
    await get().fetchPsychologists();
  },

  setPsychologistActive: async (psychologistId, active) => {
    await apiClient.put(
      `/admin/psychologists/${psychologistId}/${active ? "activate" : "deactivate"}`,
    );
    await get().fetchPsychologists();
  },

  fetchAssignments: async () => {
    const { data } = await apiClient.get<Assignment[]>("/admin/assignments");
    set({ assignments: data });
  },

  createAssignment: async (request) => {
    await apiClient.post("/admin/assignments", request);
    await get().fetchAssignments();
  },

  deleteAssignment: async (assignmentId) => {
    await apiClient.delete(`/admin/assignments/${assignmentId}`);
    await get().fetchAssignments();
  },

  fetchDataDeletionRequests: async () => {
    const { data } = await apiClient.get<AdminDataDeletionRequest[]>(
      "/admin/data-deletion-requests",
    );
    set({ dataDeletionRequests: data });
  },

  approveDataDeletionRequest: async (id) => {
    await apiClient.put(`/admin/data-deletion-requests/${id}/approve`);
    await get().fetchDataDeletionRequests();
  },
}));
