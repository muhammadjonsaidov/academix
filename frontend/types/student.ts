// Hand-written, mirrors academix_tz.md §2.4 exactly — no codegen (see CLAUDE.md known gaps).

export type StudentSubmissionStatusLabel = "PENDING" | "SUBMITTED" | "GRADED";

export interface MyTask {
  taskContent: string;
}

export interface StudentHomework {
  assignmentId: string;
  subject: string;
  title: string;
  deadlineAt: string;
  isLate: boolean;
  myTask: MyTask | null;
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

// academix_tz.md §2.4 GET /student/progress — streakHistory always empty, see backend's
// StudentProgressService Javadoc (no streak-history log exists, only current/max snapshots).
export interface StudentSubjectProgress {
  subject: string;
  currentAvg: number;
  previousMonthAvg: number;
  growth: number;
  submissionRate: number;
  trend: string;
}

export interface StudentProgress {
  xpHistory: XpHistoryItem[];
  subjectStats: StudentSubjectProgress[];
  badges: Badge[];
  streakHistory: string[];
  myGrowth: {
    thisMonth: { avgScore: number };
    lastMonth: { avgScore: number };
    growth: string;
  };
}

// Exams: gap-fill deviation, judgment call — no student-facing exam-results endpoint exists in
// academix_tz.md (see backend's StudentExamController / CLAUDE.md). handwritingMatchScore is
// deliberately never exposed, same rule as StudentAiFeedback above.

export interface StudentExamGrade {
  score: number;
  fivePointGrade: number;
  teacherComment: string | null;
}

export interface StudentExamListItem {
  examId: string;
  subject: string;
  title: string;
  examDate: string;
  myGrade: StudentExamGrade | null;
}

export interface StudentExamAiFeedback {
  feedback: string;
  criteriaScores: CriteriaScore[];
  stepAnalyses: StepAnalysis[];
}

export interface StudentExamDetail {
  examId: string;
  title: string;
  examDate: string;
  status: FullSubmissionStatus;
  aiFeedback: StudentExamAiFeedback | null;
  grade: StudentExamGrade | null;
}
