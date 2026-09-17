package uz.academixai.notification.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import uz.academixai.notification.domain.NotificationType;

/** Deviation — see NotificationController. One type's toggles per request. */
public record UpdateNotificationPreferenceRequest(
    @NotNull(message = "majburiy maydon") NotificationType type,
    boolean inAppEnabled,
    boolean telegramEnabled) {}
