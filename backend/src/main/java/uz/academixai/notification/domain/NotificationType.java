package uz.academixai.notification.domain;

/** academix_tz.md §1.15 — exact enum, don't add/reorder without checking the spec. */
public enum NotificationType {
  HOMEWORK_ASSIGNED,
  DEADLINE_REMINDER,
  HOMEWORK_GRADED,
  STREAK_BROKEN,
  STREAK_MILESTONE,
  BADGE_EARNED,
  PSYCHOLOGICAL_ALERT,
  LATE_SUBMISSION,
  CLASS_PROGRESS_REPORT,
  /** Retired with handwriting biometrics — never sent any more, kept so old rows still load. */
  HANDWRITING_PROFILE_RESET,
  AI_BUDGET_LOW
}
