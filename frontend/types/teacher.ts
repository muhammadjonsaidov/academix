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
