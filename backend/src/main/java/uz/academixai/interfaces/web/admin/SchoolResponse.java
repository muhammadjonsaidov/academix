package uz.academixai.interfaces.web.admin;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.School;

/** academix_tz.md §2.2 {@code GET/PUT /admin/school}. */
public record SchoolResponse(
    UUID id,
    String name,
    String address,
    String region,
    String district,
    String phone,
    String email,
    int totalClasses,
    boolean isActive,
    LocalDateTime subscribedAt,
    LocalDateTime subscriptionEndsAt,
    int monthlyAiCallLimit,
    int currentMonthAiUsage) {

  public static SchoolResponse from(School school) {
    return new SchoolResponse(
        school.id(),
        school.name(),
        school.address(),
        school.region(),
        school.district(),
        school.phone(),
        school.email(),
        school.totalClasses(),
        school.isActive(),
        school.subscribedAt(),
        school.subscriptionEndsAt(),
        school.monthlyAiCallLimit(),
        school.currentMonthAiUsage());
  }
}
