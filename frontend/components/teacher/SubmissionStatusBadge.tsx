import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import type { SubmissionStatus } from "@/types/teacher";

export const SUBMISSION_STATUS_LABEL: Record<SubmissionStatus, string> = {
  SUBMITTED: "Topshirilgan",
  AI_PROCESSING: "AI tahlil qilmoqda",
  AI_DONE: "AI baholadi",
  AI_SKIPPED: "AI o'tkazib yubordi",
  GRADED: "Baholangan",
};

/** Grading-rail utility class (globals.css) for a given submission status — pair with
 * `rail-flagged`/`rail-critical` by the call site when the row also carries that flag. */
export function submissionRailClass(status: SubmissionStatus): string {
  switch (status) {
    case "AI_DONE":
    case "GRADED":
      return "rail-verified";
    case "AI_SKIPPED":
      return "rail-skipped";
    case "SUBMITTED":
    case "AI_PROCESSING":
    default:
      return "rail-processing";
  }
}

export function SubmissionStatusBadge({ status }: { status: SubmissionStatus }) {
  if (status === "AI_PROCESSING" || status === "SUBMITTED") {
    return (
      <Badge variant="status-processing" className="gap-1.5">
        <Spinner />
        {SUBMISSION_STATUS_LABEL[status]}
      </Badge>
    );
  }
  if (status === "AI_SKIPPED") {
    return <Badge variant="status-skipped">{SUBMISSION_STATUS_LABEL[status]}</Badge>;
  }
  return <Badge variant="status-done">{SUBMISSION_STATUS_LABEL[status]}</Badge>;
}
