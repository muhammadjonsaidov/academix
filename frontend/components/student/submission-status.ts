import type { FullSubmissionStatus, StudentSubmissionStatusLabel } from "@/types/student";
import type { badgeVariants } from "@/components/ui/badge";
import type { VariantProps } from "class-variance-authority";

type BadgeVariant = NonNullable<VariantProps<typeof badgeVariants>["variant"]>;

interface StatusMeta {
  label: string;
  badgeVariant: BadgeVariant;
  /** Grading-rail utility class (globals.css) for cards representing this piece of work. */
  rail: string;
}

// Full AI pipeline status (submissions list/detail, exam list/detail) — mirrors the
// rail-*/status-* tokens documented by the visual-system stage (rail-verified for
// AI_DONE/GRADED, rail-processing dashed for AI_PROCESSING, rail-skipped hatched for
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

// Homework list's simpler pre-AI status (PENDING/SUBMITTED/GRADED, see StudentHomework).
export const HOMEWORK_STATUS_META: Record<StudentSubmissionStatusLabel, StatusMeta> = {
  PENDING: { label: "Topshirilmagan", badgeVariant: "outline", rail: "" },
  SUBMITTED: { label: "Topshirilgan", badgeVariant: "status-processing", rail: "rail-processing" },
  GRADED: { label: "Baholangan", badgeVariant: "ready", rail: "rail-verified" },
};
