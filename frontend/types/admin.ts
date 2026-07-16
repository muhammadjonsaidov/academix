// Hand-written, mirrors academix_tz.md §2.2 exactly — no codegen (see CLAUDE.md known gaps).

export interface SchoolClass {
  id: string;
  grade: number;
  letter: string;
  fullName: string;
  classTeacherId: string | null;
  studentCount: number;
  academicYear: string;
  isActive: boolean;
}

export interface CreateClassRequest {
  grade: number;
  letter: string;
  classTeacherId?: string;
}

export interface Teacher {
  id: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  isActive: boolean;
}

export interface InviteTeacherRequest {
  phone: string;
  firstName: string;
  lastName: string;
  email?: string;
}

export interface Student {
  id: string;
  firstName: string;
  lastName: string;
  phone: string;
  classId: string | null;
  studentNumber: string | null;
  birthDate: string | null;
  isActive: boolean;
}

export interface CreateStudentRequest {
  firstName: string;
  lastName: string;
  phone: string;
  classId: string;
  studentNumber?: string;
  birthDate?: string;
}

// academix_tz.md §2.2 "Maktab" — GET/PUT /admin/school.
export interface School {
  id: string;
  name: string;
  address: string;
  region: string;
  district: string;
  phone: string | null;
  email: string | null;
  totalClasses: number;
  isActive: boolean;
  subscribedAt: string | null;
  subscriptionEndsAt: string | null;
  monthlyAiCallLimit: number;
  currentMonthAiUsage: number;
}

export interface UpdateSchoolRequest {
  name: string;
  address: string;
  region: string;
  district: string;
  phone: string | null;
}
