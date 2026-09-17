package uz.academixai.notification.adapter.in.web;

import uz.academixai.notification.domain.NotificationPreference;
import uz.academixai.notification.domain.NotificationType;

/** Deviation — see NotificationController. {@code locked} marks types that can't be muted. */
public record NotificationPreferenceResponse(
    NotificationType type, boolean inAppEnabled, boolean telegramEnabled, boolean locked) {

  public static NotificationPreferenceResponse from(NotificationPreference pref) {
    return new NotificationPreferenceResponse(
        pref.type(),
        pref.inAppEnabled(),
        pref.telegramEnabled(),
        pref.type() == NotificationType.PSYCHOLOGICAL_ALERT);
  }
}
