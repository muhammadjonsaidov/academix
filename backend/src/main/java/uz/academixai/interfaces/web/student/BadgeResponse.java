package uz.academixai.interfaces.web.student;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.application.StudentDashboardService.DashboardBadge;
import uz.academixai.domain.Badge;

public record BadgeResponse(
    UUID id, String name, String description, String icon, LocalDateTime awardedAt) {

  public static BadgeResponse from(DashboardBadge dashboardBadge) {
    Badge badge = dashboardBadge.badge();
    return new BadgeResponse(
        badge.id(), badge.name(), badge.description(), badge.icon(), dashboardBadge.awardedAt());
  }
}
