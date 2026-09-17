package uz.academixai.school.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.School;
import uz.academixai.school.application.port.out.SchoolAdministrationRepository;
import uz.academixai.shared.error.ApiException;

/** School aggregate administration use cases for the authenticated school admin. */
@Service
public class SchoolAdministrationService {

  private final SchoolAdministrationRepository schools;

  public SchoolAdministrationService(SchoolAdministrationRepository schools) {
    this.schools = schools;
  }

  public School get(UUID schoolId) {
    School school = schools.findById(schoolId).orElseThrow(SchoolAdministrationService::notFound);
    // The denormalized total_classes column is not authoritative. Read the live value at the
    // aggregate boundary so every adapter returns the same number.
    return new School(
        school.id(),
        school.name(),
        school.address(),
        school.region(),
        school.district(),
        school.phone(),
        school.email(),
        (int) schools.countActiveClasses(schoolId),
        school.isActive(),
        school.subscribedAt(),
        school.subscriptionEndsAt(),
        school.adminId(),
        school.monthlyAiCallLimit(),
        school.currentMonthAiUsage());
  }

  public School update(
      UUID schoolId, String name, String address, String region, String district, String phone) {
    School existing = get(schoolId);
    School updated =
        new School(
            existing.id(),
            name,
            address,
            region,
            district,
            phone,
            existing.email(),
            existing.totalClasses(),
            existing.isActive(),
            existing.subscribedAt(),
            existing.subscriptionEndsAt(),
            existing.adminId(),
            existing.monthlyAiCallLimit(),
            existing.currentMonthAiUsage());
    return schools.save(updated);
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_SCHOOL_NOT_FOUND", "Maktab topilmadi.", "ID ni tekshiring.");
  }
}
