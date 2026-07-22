package uz.academixai.interfaces.web.notifications;

import uz.academixai.domain.NotificationPreference;
import uz.academixai.domain.NotificationType;

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
