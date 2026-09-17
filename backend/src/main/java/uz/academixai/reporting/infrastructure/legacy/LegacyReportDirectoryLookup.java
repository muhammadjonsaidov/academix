package uz.academixai.reporting.infrastructure.legacy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.reporting.application.port.out.ReportDirectoryLookup;

/** Adapter for {@link ReportDirectoryLookup} over the legacy school/class/user repositories. */
@Component
public class LegacyReportDirectoryLookup implements ReportDirectoryLookup {

  private final SchoolRepository schools;
  private final SchoolClassRepository classes;
  private final UserRepository users;

  public LegacyReportDirectoryLookup(
      SchoolRepository schools, SchoolClassRepository classes, UserRepository users) {
    this.schools = schools;
    this.classes = classes;
    this.users = users;
  }

  @Override
  public Optional<String> schoolName(UUID schoolId) {
    return schools.findById(schoolId).map(entity -> entity.toDomain().name());
  }

  @Override
  public Optional<String> classLabel(UUID classId, UUID schoolId) {
    return classes
        .findByIdAndSchoolId(classId, schoolId)
        .map(entity -> entity.toDomain().fullName());
  }

  @Override
  public Optional<String> studentFullName(UUID studentUserId) {
    return users
        .findById(studentUserId)
        .map(entity -> entity.toDomain().firstName() + " " + entity.toDomain().lastName());
  }
}
