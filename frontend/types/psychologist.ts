// Hand-written, mirrors academix_tz.md §2.6 — no codegen (see CLAUDE.md known gaps). Some
// shapes deviate from the spec where it's silent (see backend's PsychologistService).

export type SignalSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type SignalType =
  | "LATE_NIGHT_ACTIVITY"
  | "MOTIVATION_DROP"
  | "NEGATIVE_LANGUAGE"
  | "SUDDEN_PERFORMANCE_DROP"
  | "SUBMISSION_STOP"
  | "AGGRESSIVE_LANGUAGE"
  | "MANIPULATION_ATTEMPT";

export interface WatchlistStudent {
  studentId: string;
  studentName: string;
  reason: string | null;
  addedAt: string;
}

export interface PsychologistDashboard {
  criticalSignals: number;
  highSignals: number;
  mediumSignals: number;
  resolvedThisWeek: number;
  watchlistStudents: WatchlistStudent[];
}

export interface PsychologistSignalListItem {
  signalId: string;
  studentName: string;
  studentId: string;
  className: string;
  type: SignalType;
  severity: SignalSeverity;
  description: string | null;
  detectedAt: string;
  isManipulation: boolean;
}

export interface XpTrendEntry {
  id: string;
  studentId: string;
  xp: number;
  reason: string;
  occurredAt: string;
}

export interface SubmissionDay {
  date: string;
  count: number;
}

export interface BehaviorProfile {
  activeHours: Record<string, number>;
  xpTrend: XpTrendEntry[];
  submissionPattern: SubmissionDay[];
  keyPhrases: string[];
}

export interface PsychologistSignalDetail {
  signal: {
    signalId: string;
    studentId: string;
    studentName: string;
    className: string;
    type: SignalType;
    severity: SignalSeverity;
    description: string | null;
    isManipulation: boolean;
    resolved: boolean;
    detectedAt: string;
    resolvedAt: string | null;
    resolutionNotes: string | null;
    actionTaken: string | null;
  };
  studentBehaviorProfile: BehaviorProfile;
}

export interface ResolveSignalRequest {
  notes: string;
  actionTaken: string;
}

export interface MonthlyReport {
  totalSignals: number;
  bySeverity: Record<string, number>;
  byType: Record<string, number>;
  resolvedCount: number;
  manipulationFlaggedCount: number;
}
