package uz.academixai.application;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.School;
import uz.academixai.infrastructure.persistence.SchoolEntity;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.interfaces.web.ApiException;

/** academix_tz.md §2.2 "Maktab" — GET/PUT /admin/school. */
@Service
public class AdminSchoolService {

  private final SchoolRepository schoolRepository;

  public AdminSchoolService(SchoolRepository schoolRepository) {
    this.schoolRepository = schoolRepository;
  }

  public School get(UUID schoolId) {
    return schoolRepository
        .findById(schoolId)
        .map(SchoolEntity::toDomain)
        .orElseThrow(AdminSchoolService::notFound);
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
    return schoolRepository.save(SchoolEntity.fromDomain(updated)).toDomain();
  }

  private static ApiException notFound() {
    return new ApiException(
        HttpStatus.NOT_FOUND, "ERR_SCHOOL_NOT_FOUND", "Maktab topilmadi.", "ID ni tekshiring.");
  }
}
