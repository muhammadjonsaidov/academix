import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { PageResponse } from "@/types/api";
import type {
  AdminDataDeletionRequest,
  Assignment,
  CreateAssignmentRequest,
  CreateClassRequest,
  CreateParentRequest,
  CreateStudentRequest,
  CreateSubjectRequest,
  InvitePsychologistRequest,
  InviteTeacherRequest,
  LinkParentRequest,
  Parent,
  ParentCheck,
  ParentLink,
  Psychologist,
  School,
  SchoolClass,
  Student,
  Subject,
  Teacher,
  UpdateClassRequest,
  UpdateSchoolRequest,
} from "@/types/admin";

interface StudentFilters {
  classId?: string;
  search?: string;
}

interface AdminState {
  classes: SchoolClass[];
  teachers: Teacher[];
  students: Student[];
  studentsPage: number;
  studentsPageSize: number;
  studentsTotalItems: number;
  /** Last filters passed to fetchStudents — reused by the page/size setters. */
  studentsFilters: StudentFilters;
  /** Full (unpaginated) student list for dropdowns/name lookups — kept separate so
   * the paginated students page can't clobber it. Filled by fetchAllStudents. */
  allStudents: Student[];
  school: School | null;
  psychologists: Psychologist[];
  assignments: Assignment[];
  subjects: Subject[];
  parents: Parent[];
  dataDeletionRequests: AdminDataDeletionRequest[];

  fetchClasses: () => Promise<void>;
  createClass: (request: CreateClassRequest) => Promise<void>;
  updateClass: (classId: string, request: UpdateClassRequest) => Promise<void>;
  deleteClass: (classId: string) => Promise<void>;

  fetchTeachers: () => Promise<void>;
  inviteTeacher: (request: InviteTeacherRequest) => Promise<void>;
  setTeacherActive: (teacherId: string, active: boolean) => Promise<void>;

  fetchStudents: (filters?: StudentFilters) => Promise<void>;
  setStudentsPage: (page: number) => Promise<void>;
  setStudentsPageSize: (size: number) => Promise<void>;
  fetchAllStudents: () => Promise<void>;
  createStudent: (request: CreateStudentRequest) => Promise<void>;
  transferStudentClass: (studentId: string, newClassId: string) => Promise<void>;
  unlockHandwritingReset: (studentId: string) => Promise<void>;
  linkParentToStudent: (request: LinkParentRequest) => Promise<ParentLink>;

  fetchParents: () => Promise<void>;
  createParent: (request: CreateParentRequest) => Promise<void>;
  checkParentPhone: (phone: string) => Promise<ParentCheck>;

  fetchSchool: () => Promise<void>;
  updateSchool: (request: UpdateSchoolRequest) => Promise<void>;

  fetchPsychologists: () => Promise<void>;
  invitePsychologist: (request: InvitePsychologistRequest) => Promise<void>;
  setPsychologistActive: (psychologistId: string, active: boolean) => Promise<void>;

  fetchAssignments: () => Promise<void>;
  createAssignment: (request: CreateAssignmentRequest) => Promise<void>;
  deleteAssignment: (assignmentId: string) => Promise<void>;

  fetchSubjects: () => Promise<void>;
  createSubject: (request: CreateSubjectRequest) => Promise<void>;
  deleteSubject: (subjectId: string) => Promise<void>;

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
  studentsPage: 0,
  studentsPageSize: 20,
  studentsTotalItems: 0,
  studentsFilters: {},
  allStudents: [],
  school: null,
  psychologists: [],
  assignments: [],
  subjects: [],
  parents: [],
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

  // New filters always restart from page 0; page/size setters below reuse the last filters.
  fetchStudents: async (filters) => {
    const { data } = await apiClient.get<PageResponse<Student>>("/admin/students", {
      params: { ...(filters ?? {}), page: 0, size: get().studentsPageSize },
    });
    set({
      students: data.items,
      studentsPage: data.page,
      studentsPageSize: data.size,
      studentsTotalItems: data.totalItems,
      studentsFilters: filters ?? {},
    });
  },

  setStudentsPage: async (page) => {
    const { studentsFilters, studentsPageSize } = get();
    const { data } = await apiClient.get<PageResponse<Student>>("/admin/students", {
      params: { ...studentsFilters, page, size: studentsPageSize },
    });
    set({
      students: data.items,
      studentsPage: data.page,
      studentsTotalItems: data.totalItems,
    });
  },

  setStudentsPageSize: async (size) => {
    const { data } = await apiClient.get<PageResponse<Student>>("/admin/students", {
      params: { ...get().studentsFilters, page: 0, size },
    });
    set({
      students: data.items,
      studentsPage: data.page,
      studentsPageSize: data.size,
      studentsTotalItems: data.totalItems,
    });
  },

  // Walks every page at the max size (100) so dropdowns/name lookups always see the
  // whole school, independent of the students page's pagination state.
  fetchAllStudents: async () => {
    const size = 100;
    const first = await apiClient.get<PageResponse<Student>>("/admin/students", {
      params: { page: 0, size },
    });
    const items = [...first.data.items];
    const totalPages = Math.ceil(first.data.totalItems / size);
    for (let page = 1; page < totalPages; page += 1) {
      const { data } = await apiClient.get<PageResponse<Student>>("/admin/students", {
        params: { page, size },
      });
      items.push(...data.items);
    }
    set({ allStudents: items });
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

  // Refreshes the parents list afterwards — a link can create/backfill a parent server-side,
  // and the old fire-and-forget version was exactly the reported "parent vanished" experience.
  linkParentToStudent: async (request) => {
    const { data } = await apiClient.post<ParentLink>("/admin/parents/link", request);
    await get().fetchParents();
    return data;
  },

  fetchParents: async () => {
    const { data } = await apiClient.get<Parent[]>("/admin/parents");
    set({ parents: data });
  },

  createParent: async (request) => {
    await apiClient.post("/admin/parents", request);
    await get().fetchParents();
  },

  checkParentPhone: async (phone) => {
    const { data } = await apiClient.get<ParentCheck>("/admin/parents/check", {
      params: { phone },
    });
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

  fetchSubjects: async () => {
    const { data } = await apiClient.get<Subject[]>("/admin/subjects");
    set({ subjects: data });
  },

  createSubject: async (request) => {
    await apiClient.post("/admin/subjects", request);
    await get().fetchSubjects();
  },

  deleteSubject: async (subjectId) => {
    await apiClient.delete(`/admin/subjects/${subjectId}`);
    await get().fetchSubjects();
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
