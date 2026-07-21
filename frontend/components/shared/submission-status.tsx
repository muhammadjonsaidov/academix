import { Badge, type badgeVariants } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import type { VariantProps } from "class-variance-authority";

type BadgeVariant = NonNullable<VariantProps<typeof badgeVariants>["variant"]>;

/** The AI-grading-pipeline status shared by homework/exam submissions across every role
 * that renders one (teacher's `SubmissionStatus`, student's `FullSubmissionStatus` —
 * structurally identical string unions, kept as separate hand-written types per role
 * in types/{teacher,student}.ts, but backed by this single source of truth for how each
 * value renders). */
export type FullSubmissionStatus = "SUBMITTED" | "AI_PROCESSING" | "AI_DONE" | "AI_SKIPPED" | "GRADED";

interface StatusMeta {
  label: string;
  badgeVariant: BadgeVariant;
  /** Grading-rail utility class (globals.css) for cards/rows representing this work. */
  rail: string;
}

// Mirrors the rail-*/status-* tokens documented by the visual-system stage (rail-verified
// for AI_DONE/GRADED, rail-processing dashed for AI_PROCESSING, rail-skipped hatched for
// AI_SKIPPED). SUBMITTED has no AI verdict yet, so it gets a neutral outline + no rail.
export const FULL_STATUS_META: Record<FullSubmissionStatus, StatusMeta> = {
  SUBMITTED: { label: "Topshirilgan", badgeVariant: "outline", rail: "" },
  AI_PROCESSING: {
    label: "AI tahlil qilmoqda",
    badgeVariant: "status-processing",
    rail: "rail-processing",
  },
  AI_DONE: { label: "AI baholadi", badgeVariant: "status-done", rail: "rail-verified" },
  AI_SKIPPED: {
    label: "Qo'lda baholanadi",
    badgeVariant: "status-skipped",
    rail: "rail-skipped",
  },
  GRADED: { label: "Baholangan", badgeVariant: "ready", rail: "rail-verified" },
};

/** Homework list's simpler pre-AI status (PENDING/SUBMITTED/GRADED, see StudentHomework). */
export type HomeworkStatus = "PENDING" | "SUBMITTED" | "GRADED";

export const HOMEWORK_STATUS_META: Record<HomeworkStatus, StatusMeta> = {
  PENDING: { label: "Topshirilmagan", badgeVariant: "outline", rail: "" },
  SUBMITTED: { label: "Topshirilgan", badgeVariant: "status-processing", rail: "rail-processing" },
  GRADED: { label: "Baholangan", badgeVariant: "ready", rail: "rail-verified" },
};

/** Grading-rail utility class for a given full-pipeline status — thin wrapper over
 * FULL_STATUS_META for call sites that only need the rail class, not the full meta. */
export function submissionRailClass(status: FullSubmissionStatus): string {
  return FULL_STATUS_META[status].rail;
}

/** Status badge with a spinner while AI work is in flight — built on the same
 * FULL_STATUS_META every other role/screen reads for label/variant, so there's one
 * source of truth for what each status means. */
export function SubmissionStatusBadge({ status }: { status: FullSubmissionStatus }) {
  const meta = FULL_STATUS_META[status];
  if (status === "AI_PROCESSING" || status === "SUBMITTED") {
    return (
      <Badge variant={meta.badgeVariant} className="gap-1.5">
        <Spinner />
        {meta.label}
      </Badge>
    );
  }
  return <Badge variant={meta.badgeVariant}>{meta.label}</Badge>;
}
