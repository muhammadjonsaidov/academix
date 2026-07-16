// Hand-written, mirrors academix_tz.md §2.5 — no codegen (see CLAUDE.md known gaps). Some
// shapes deviate from the spec where it's silent (see backend's ParentDashboardService /
// ParentProgressService).

export interface RecentGrade {
  submissionId: string;
  score: number;
  fivePointGrade: number;
}

export interface ChildSummary {
  studentId: string;
  name: string;
  className: string;
  todayActivity: boolean;
  pendingHomeworkCount: number;
  recentGrade: RecentGrade | null;
  biometricConsentGiven: boolean;
}

export interface ParentDashboard {
  children: ChildSummary[];
}

export interface StudentHomeworkItem {
  assignmentId: string;
  subjectName: string;
  title: string;
  deadlineAt: string;
  isLate: boolean;
  submissionStatus: string;
  myTaskContent: string | null;
}

export interface ChildOverview {
  summary: ChildSummary;
  pendingHomework: StudentHomeworkItem[];
  recentGrades: RecentGrade[];
}

export interface ParentSubmission {
  submissionId: string;
  assignmentId: string;
  status: string;
  isLate: boolean;
  submittedAt: string;
}

export interface SubjectProgress {
  subject: string;
  currentAvg: number;
  previousMonthAvg: number;
  growth: number;
  submissionRate: number;
  trend: "UP" | "DOWN" | "STABLE";
}

export interface MonthlyXp {
  month: string;
  xp: number;
}

export interface BadgeItem {
  name: string;
  icon: string;
  awardedAt: string;
}

export interface ParentProgress {
  subjectProgress: SubjectProgress[];
  monthlyXpChart: MonthlyXp[];
  badges: BadgeItem[];
}

export interface ParentGradeItem {
  submissionId: string;
  subject: string;
  score: number;
  fivePointGrade: number;
}

export interface ParentGrades {
  recent: ParentGradeItem[];
  bySubject: Record<string, number>;
}

export interface SetBiometricConsentRequest {
  consentGiven: boolean;
}

export interface DataDeletionRequestResponse {
  id: string;
  studentId: string;
  status: string;
  requestedAt: string;
}
