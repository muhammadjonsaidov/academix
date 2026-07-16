// Hand-written, mirrors academix_tz.md §2.4 exactly — no codegen (see CLAUDE.md known gaps).

export type StudentSubmissionStatusLabel = "PENDING" | "SUBMITTED" | "GRADED";

export interface StudentHomework {
  assignmentId: string;
  subject: string;
  title: string;
  deadlineAt: string;
  isLate: boolean;
  submissionStatus: StudentSubmissionStatusLabel;
}

export type SubmissionType = "TEXT" | "IMAGE" | "MIXED";

export interface SubmitHomeworkResponse {
  submissionId: string;
  status: string;
  message: string;
}

export interface CriteriaScore {
  name: string;
  weightPercent: number;
  score: number;
}

export interface StepAnalysis {
  stepNumber: number;
  stepContent: string;
  isCorrect: boolean;
  errorDescription: string | null;
  suggestion: string | null;
}

export interface StudentAiFeedback {
  feedback: string;
  criteriaScores: CriteriaScore[];
  stepAnalyses: StepAnalysis[];
}

export interface StudentGrade {
  score: number;
  fivePointGrade: number;
  teacherComment: string | null;
}

export type FullSubmissionStatus = "SUBMITTED" | "AI_PROCESSING" | "AI_DONE" | "AI_SKIPPED" | "GRADED";

export interface StudentSubmissionListItem {
  submissionId: string;
  assignmentId: string;
  status: FullSubmissionStatus;
  isLate: boolean;
  submittedAt: string;
}

export interface StudentSubmissionDetail {
  submissionId: string;
  assignmentId: string;
  status: FullSubmissionStatus;
  isLate: boolean;
  submittedAt: string;
  aiFeedback: StudentAiFeedback | null;
  grade: StudentGrade | null;
}

export interface Badge {
  id: string;
  name: string;
  description: string;
  icon: string;
  awardedAt: string;
}

export interface RecentGrade {
  submissionId: string;
  score: number;
  fivePointGrade: number;
  gradedAt: string;
}

export interface ProfileSummary {
  firstName: string;
  totalXp: number;
  currentStreak: number;
  maxStreak: number;
}

export interface StudentDashboard {
  profile: ProfileSummary;
  badges: Badge[];
  pendingHomework: StudentHomework[];
  recentGrades: RecentGrade[];
  xpToNextBadge: number;
}

export interface XpHistoryItem {
  date: string;
  xp: number;
  reason: string;
}
