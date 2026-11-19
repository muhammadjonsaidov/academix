// Hand-written, mirrors academix_tz.md §2.3 exactly — no codegen (see CLAUDE.md known gaps).

export interface TeacherClass {
  id: string;
  fullName: string;
  studentCount: number;
}

export interface TeacherStudent {
  id: string;
  firstName: string;
  lastName: string;
  studentNumber: string | null;
}

export interface TeacherSubject {
  id: string;
  name: string;
}

export interface Homework {
  id: string;
  classId: string;
  subjectId: string;
  title: string;
  description: string | null;
  assignedAt: string;
  deadlineAt: string;
  maxScore: number;
  isActive: boolean;
  syllabusReference: string | null;
}

export interface CreateHomeworkRequest {
  classId: string;
  subjectId: string;
  title: string;
  description: string;
  deadlineAt: string;
  syllabusReference?: string;
  maxScore: number;
}

// academix_tz.md §2.3 — PUT /teacher/homework/{assignmentId} body. classId/subjectId
// aren't editable (backend's UpdateHomeworkRequest doesn't accept them).
export interface UpdateHomeworkRequest {
  title: string;
  description: string;
  deadlineAt: string;
  syllabusReference?: string | null;
  maxScore: number;
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

export type PlagiarismType = "CLEAN" | "AI_GENERATED" | "HANDWRITING_MISMATCH" | "SUSPICIOUS";

export interface TeacherAiFeedback {
  extractedText: string;
  aiScorePercent: number;
  criteriaScores: CriteriaScore[];
  stepAnalyses: StepAnalysis[];
  plagiarismScore: number;
  plagiarismType: PlagiarismType;
  feedback: string;
}

export interface TeacherGrade {
  score: number;
  fivePointGrade: number;
  teacherComment: string | null;
  teacherOverrodeAI: boolean;
}

export type SubmissionStatus = "SUBMITTED" | "AI_PROCESSING" | "AI_DONE" | "AI_SKIPPED" | "GRADED";

export interface TeacherSubmissionListItem {
  submissionId: string;
  studentName: string;
  studentId: string;
  assignmentId: string;
  assignmentTitle: string | null;
  submittedAt: string;
  isLate: boolean;
  status: SubmissionStatus;
  aiFeedback: TeacherAiFeedback | null;
  /** 5-point grade matching the AI score (86+ → 5, 71+ → 4, 56+ → 3); a suggestion only. */
  suggestedGrade: number | null;
}

/** GET /teacher/submissions/review-queue — one row per assignment with AI-checked work waiting. */
export interface ReviewQueueItem {
  assignmentId: string;
  assignmentTitle: string;
  waiting: number;
  /** Work the AI flagged as possibly copied or AI-written — needs an individual look. */
  flagged: number;
  oldestSubmittedAt: string | null;
}

export interface TeacherSubmissionDetail {
  submissionId: string;
  studentId: string;
  studentName: string;
  assignmentId: string;
  assignmentTitle: string | null;
  submittedAt: string;
  isLate: boolean;
  status: SubmissionStatus;
  originalImageUrl: string | null;
  aiFeedback: TeacherAiFeedback | null;
  previousGrade: TeacherGrade | null;
  suggestedGrade: number | null;
  /** How this subject is checked; decides what the analysis rows are called. */
  gradingMode: GradingMode | null;
}

export type GradingMode = "PROBLEM_SOLVING" | "LANGUAGE" | "WRITTEN_RESPONSE";

export interface GradeSubmissionRequest {
  score: number;
  fivePointGrade: number;
  teacherComment: string;
  /** Marks the work as exemplary: +20 XP for the student. */
  isExcellent?: boolean;
}

export interface CriteriaItem {
  name: string;
  weightPercent: number;
  description: string;
}

// Exams: academix_tz.md §2.3 "Nazorat ishi" — GET (list) is a gap-fill deviation, see
// backend's TeacherExamController / CLAUDE.md.

export interface CreateExamRequest {
  classId: string;
  subjectId: string;
  title: string;
  examDate: string;
  maxScore: number;
}

export interface CreateExamResponse {
  examId: string;
  estimatedAiCalls: number;
  remainingExamBudget: number;
  warning: string | null;
}

export interface ExamListItem {
  examId: string;
  classId: string;
  subjectId: string;
  title: string;
  examDate: string;
  maxScore: number;
  submissionsCount: number;
  gradedCount: number;
}

export interface ExamAiFeedback {
  extractedText: string;
  stepAnalyses: StepAnalysis[];
  criteriaScores: CriteriaScore[];
  aiScorePercent: number;
  feedback: string;
}

export interface ExamSubmission {
  submissionId: string;
  studentId: string;
  studentName: string;
  uploadedAt: string;
  status: SubmissionStatus;
  flaggedForReview: boolean;
  aiFeedback: ExamAiFeedback | null;
  score: number | null;
  fivePointGrade: number | null;
  teacherComment: string | null;
}

export interface BulkUploadExamSubmissionsResponse {
  queued: number;
}

export interface GradeExamSubmissionRequest {
  score: number;
  fivePointGrade: number;
  teacherComment: string;
}

// Psychological signals: academix_tz.md §2.3 "Psixologik signallar (faqat sinf rahbari
// ko'radi)" — exact paths, response shape is a deviation (see backend's
// TeacherPsychologyService).

export type SignalSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type SignalType =
  | "LATE_NIGHT_ACTIVITY"
  | "MOTIVATION_DROP"
  | "NEGATIVE_LANGUAGE"
  | "SUDDEN_PERFORMANCE_DROP"
  | "SUBMISSION_STOP"
  | "AGGRESSIVE_LANGUAGE"
  | "MANIPULATION_ATTEMPT";

export interface TeacherPsychologicalSignal {
  signalId: string;
  studentId: string;
  studentName: string;
  type: SignalType;
  severity: SignalSeverity;
  description: string | null;
  resolved: boolean;
  detectedAt: string;
  resolvedAt: string | null;
}
