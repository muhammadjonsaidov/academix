import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type {
  CreateClassRequest,
  CreateStudentRequest,
  InviteTeacherRequest,
  SchoolClass,
  Student,
  Teacher,
} from "@/types/admin";

interface AdminState {
  classes: SchoolClass[];
  teachers: Teacher[];
  students: Student[];

  fetchClasses: () => Promise<void>;
  createClass: (request: CreateClassRequest) => Promise<void>;
  deleteClass: (classId: string) => Promise<void>;

  fetchTeachers: () => Promise<void>;
  inviteTeacher: (request: InviteTeacherRequest) => Promise<void>;
  setTeacherActive: (teacherId: string, active: boolean) => Promise<void>;

  fetchStudents: (filters?: { classId?: string; search?: string }) => Promise<void>;
  createStudent: (request: CreateStudentRequest) => Promise<void>;
}

// One store for all three admin CRUD areas (classes/teachers/students) — they're a single
// Sprint 1 UI section (app/dashboard/admin/{classes,teachers,students}), not independently
// evolving domains like homework/exams, so splitting into 3 stores would just be indirection.
export const useAdminStore = create<AdminState>((set, get) => ({
  classes: [],
  teachers: [],
  students: [],

  fetchClasses: async () => {
    const { data } = await apiClient.get<SchoolClass[]>("/admin/classes");
    set({ classes: data });
  },

  createClass: async (request) => {
    await apiClient.post("/admin/classes", request);
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
}));
