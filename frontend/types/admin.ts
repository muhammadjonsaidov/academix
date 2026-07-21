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

// PUT /admin/classes/{classId} — same request shape as create (AdminClassController#update
// reuses CreateClassRequest on the backend), aliased separately here for call-site clarity.
export type UpdateClassRequest = CreateClassRequest;

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

// PUT /admin/students/{studentId}/transfer-class — { newClassId: "uuid" }
export interface TransferClassRequest {
  newClassId: string;
}

// academix_tz.md §1.7 — exact enum (domain.ParentRelation).
export type ParentRelation = "MOTHER" | "FATHER" | "GUARDIAN";

// POST /admin/parents/link request body — exact shape from AdminParentLinkController.
export interface LinkParentRequest {
  parentPhone: string;
  studentId: string;
  relation: ParentRelation;
}

export interface ParentLink {
  id: string;
  parentUserId: string;
  studentUserId: string;
  relation: ParentRelation;
  biometricConsentGiven: boolean;
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

// Deviation, judgment call (see backend PsychologistManagementService's Javadoc) — mirrors
// Teacher/InviteTeacherRequest exactly, just scoped to Role.PSYCHOLOGIST.
export interface Psychologist {
  id: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  isActive: boolean;
}

export interface InvitePsychologistRequest {
  phone: string;
  firstName: string;
  lastName: string;
  email?: string;
}

// academix_tz.md §2.2 "O'qituvchi-Sinf-Fan biriktirish" — AdminAssignmentController's exact
// shape. No admin-facing subjects-catalog endpoint exists anywhere (GET /teacher/subjects is
// hasRole('TEACHER')-gated and only ever derives from that teacher's *existing* assignments —
// checked against TeacherContextService.mySubjects, a chicken-and-egg source, unusable here) —
// flagged as a further gap, not invented.
export interface Assignment {
  id: string;
  teacherId: string;
  classId: string;
  subjectId: string;
  academicYear: string;
}

export interface CreateAssignmentRequest {
  teacherId: string;
  classId: string;
  subjectId: string;
}

// academix_tz.md §2.7 — admin-side approval queue. Mirrors backend AdminDataDeletionController's
// DataDeletionRequestResponse exactly (superset of the parent-facing DTO in types/parent.ts,
// which omits requestedBy/approvedAt).
export type DeletionRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface AdminDataDeletionRequest {
  id: string;
  studentId: string;
  requestedBy: string;
  status: DeletionRequestStatus;
  requestedAt: string;
  approvedAt: string | null;
}
