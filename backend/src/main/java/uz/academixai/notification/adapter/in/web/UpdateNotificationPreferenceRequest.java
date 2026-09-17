package uz.academixai.notification.adapter.in.web;

import uz.academixai.notification.domain.NotificationType;

/** Deviation — see NotificationController. One type's toggles per request. */
public record UpdateNotificationPreferenceRequest(
    NotificationType type, boolean inAppEnabled, boolean telegramEnabled) {}
