package uz.academixai.domain;

/**
 * academix_tz.md §1.10 — {@code AI_SKIPPED} is the graceful-degradation status when the school's
 * monthly AI budget is exhausted: OCR still runs, grading doesn't, teacher grades manually.
 */
public enum SubmissionStatus {
  SUBMITTED,
  AI_PROCESSING,
  AI_DONE,
  AI_SKIPPED,
  GRADED
}
