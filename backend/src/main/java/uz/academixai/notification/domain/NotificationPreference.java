package uz.academixai.notification.domain;

import java.util.UUID;

/**
 * Per-user, per-type notification channel toggles (V41 migration — deviation, no preferences
 * concept in academix_tz.md). A user with no stored row for a type gets both channels enabled.
 * {@link NotificationType#PSYCHOLOGICAL_ALERT} is exempt from muting entirely — the TZ §1.14
 * severity→notify matrix is a safety rule (CRITICAL must reach the parent), so preferences are
 * ignored for that type at send time.
 */
public record NotificationPreference(
    UUID id, UUID userId, NotificationType type, boolean inAppEnabled, boolean telegramEnabled) {}
