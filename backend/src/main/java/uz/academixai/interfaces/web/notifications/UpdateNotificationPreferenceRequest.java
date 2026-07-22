package uz.academixai.interfaces.web.notifications;

import uz.academixai.domain.NotificationType;

/** Deviation — see NotificationController. One type's toggles per request. */
public record UpdateNotificationPreferenceRequest(
    NotificationType type, boolean inAppEnabled, boolean telegramEnabled) {}
