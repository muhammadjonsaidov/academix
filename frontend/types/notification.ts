// Deviation — no inbox response shape is documented in academix_tz.md, see backend
// NotificationController's own Javadoc for the same flag.
export type NotificationType =
  | "HOMEWORK_ASSIGNED"
  | "DEADLINE_REMINDER"
  | "HOMEWORK_GRADED"
  | "STREAK_BROKEN"
  | "STREAK_MILESTONE"
  | "BADGE_EARNED"
  | "PSYCHOLOGICAL_ALERT"
  | "LATE_SUBMISSION"
  | "CLASS_PROGRESS_REPORT"
  | "HANDWRITING_PROFILE_RESET"
  | "AI_BUDGET_LOW";

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  body: string | null;
  data: string | null;
  isRead: boolean;
  sentToTelegram: boolean;
  createdAt: string;
  readAt: string | null;
}

// Mirrors backend NotificationPreferenceResponse — `locked` types (psychological alerts)
// can't be muted, per the TZ §1.14 severity→notify safety matrix.
export interface NotificationPreference {
  type: NotificationType;
  inAppEnabled: boolean;
  telegramEnabled: boolean;
  locked: boolean;
}
