// Hand-written, mirrors academix_tz.md §2.3 exactly — no codegen (see CLAUDE.md known gaps).

export interface TeacherClass {
  id: string;
  fullName: string;
  studentCount: number;
}

export interface TeacherSubject {
  id: string;
  name: string;
}

export type AssignmentType = "STANDARD" | "UNIQUE_GENERATED";

export interface Homework {
  id: string;
  classId: string;
  subjectId: string;
  title: string;
  description: string | null;
  type: AssignmentType;
  assignedAt: string;
  deadlineAt: string;
  maxScore: number;
  isActive: boolean;
  syllabusReference: string | null;
  tasksPublished: boolean;
}

export interface CreateHomeworkRequest {
  classId: string;
  subjectId: string;
  title: string;
  description: string;
  deadlineAt: string;
  type: AssignmentType;
  syllabusReference?: string;
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
  handwritingMatchScore: number;
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
  submittedAt: string;
  isLate: boolean;
  status: SubmissionStatus;
  aiFeedback: TeacherAiFeedback | null;
}

export interface TeacherSubmissionDetail {
  submissionId: string;
  studentId: string;
  studentName: string;
  submittedAt: string;
  isLate: boolean;
  status: SubmissionStatus;
  originalImageUrl: string | null;
  aiFeedback: TeacherAiFeedback | null;
  previousGrade: TeacherGrade | null;
}

export interface GradeSubmissionRequest {
  score: number;
  fivePointGrade: number;
  teacherComment: string;
}

export type FileType = "PDF" | "DOCX" | "IMAGE";

export interface Syllabus {
  id: string;
  subjectId: string;
  classId: string;
  title: string;
  fileUrl: string;
  fileType: FileType;
  isProcessed: boolean;
  uploadedAt: string;
}

export interface LessonActivity {
  description: string;
  durationMinutes: number;
}

export interface LessonPlanContent {
  objectives: string[];
  activities: LessonActivity[];
  materials: string[];
  homeworkSuggestion: string;
}

export interface LessonPlan {
  lessonPlanId: string;
  subjectId: string;
  classId: string;
  syllabusId: string;
  topic: string;
  aiGeneratedPlan: LessonPlanContent | null;
  teacherEditedPlan: string | null;
  isApproved: boolean;
  lessonDate: string;
  createdAt: string;
}

export interface GenerateLessonPlanRequest {
  syllabusId: string;
  topic: string;
  lessonDate: string;
  classId: string;
}

export interface UpdateLessonPlanRequest {
  teacherEditedPlan: string;
  isApproved: boolean;
}

export interface CriteriaItem {
  name: string;
  weightPercent: number;
  description: string;
}

export interface UniqueTask {
  taskId: string;
  studentId: string;
  studentName: string;
  taskContent: string;
  isApproved: boolean;
  flaggedForReview: boolean;
  fallbackToStandard: boolean;
}

export type ResetReason = "ILLNESS" | "INJURY" | "TRANSFER_STUDENT" | "OTHER";

export interface HandwritingResetRequest {
  reason: ResetReason;
  notes?: string;
}

export interface HandwritingResetResponse {
  newProfileVersion: string;
  resetCountThisSemester: number;
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
  handwritingMatchScore: number;
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
