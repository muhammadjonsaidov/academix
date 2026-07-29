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

// --- Admin parent lifecycle (deviation beyond §2.2, see backend ParentManagementService) ---

export interface LinkedChild {
  studentId: string;
  firstName: string;
  lastName: string;
  className: string | null;
  relation: ParentRelation;
}

// GET /admin/parents — school-scoped parents with their actively-linked children.
export interface Parent {
  id: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  isActive: boolean;
  children: LinkedChild[];
}

// POST /admin/parents — phone AND email both required (email is the password-reset channel);
// password is the admin-chosen initial password handed to the parent.
export interface CreateParentRequest {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  password: string;
}

// GET /admin/parents/check?phone=... — pre-link lookup: does this phone already belong to
// someone, and (if a parent) which children are already linked.
export interface ParentCheck {
  exists: boolean;
  role: string | null;
  userId: string | null;
  firstName: string | null;
  lastName: string | null;
  children: LinkedChild[];
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

// GET/POST/DELETE /admin/subjects — originally read-only (subjects were assumed seed/fixture
// data), now full admin CRUD: a fresh school must be manageable entirely from the UI, no seeds.
export interface Subject {
  id: string;
  name: string;
  type: string;
  icon: string | null;
}

// Mirrors backend domain.SubjectType exactly.
export const SUBJECT_TYPES = [
  "MATH",
  "LANGUAGE_UZ",
  "LANGUAGE_RU",
  "LANGUAGE_EN",
  "PHYSICS",
  "CHEMISTRY",
  "BIOLOGY",
  "HISTORY",
  "GEOGRAPHY",
  "OTHER",
] as const;
export type SubjectTypeName = (typeof SUBJECT_TYPES)[number];

export interface CreateSubjectRequest {
  name: string;
  type: SubjectTypeName;
  icon?: string;
}

// academix_tz.md §2.2 "O'qituvchi-Sinf-Fan biriktirish" — AdminAssignmentController's exact shape.
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
