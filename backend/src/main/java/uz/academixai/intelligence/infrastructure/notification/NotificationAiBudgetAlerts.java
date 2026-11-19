package uz.academixai.intelligence.infrastructure.notification;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.intelligence.application.port.out.AiBudgetAlerts;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.notification.application.NotificationService;
import uz.academixai.notification.domain.NotificationType;

/** Sends the low-budget warning to every administrator of the school. */
@Component
public class NotificationAiBudgetAlerts implements AiBudgetAlerts {

  private final UserRepository users;
  private final NotificationService notifications;

  public NotificationAiBudgetAlerts(UserRepository users, NotificationService notifications) {
    this.users = users;
    this.notifications = notifications;
  }

  @Override
  public void budgetRunningLow(UUID schoolId, AiCallCategory category, long used, int allocated) {
    String body =
        "Bu oy %s uchun AI limitining %d/%d qismi ishlatildi. Limit tugasa, ishlar AI'siz qabul qilinadi va o'qituvchi qo'lda baholaydi."
            .formatted(label(category), used, allocated);
    users
        .findByRoleAndSchoolIdOrderByLastNameAscFirstNameAsc(Role.ADMIN, schoolId)
        .forEach(
            admin ->
                notifications.sendNotification(
                    admin.getId(),
                    NotificationType.AI_BUDGET_LOW,
                    "AI limiti tugab bormoqda",
                    body,
                    Map.of("category", category.name())));
  }

  private static String label(AiCallCategory category) {
    return switch (category) {
      case HOMEWORK -> "uy vazifalarini tekshirish";
      case EXAM -> "imtihonlarni tekshirish";
      case CHAT -> "AI repetitor";
    };
  }
}
