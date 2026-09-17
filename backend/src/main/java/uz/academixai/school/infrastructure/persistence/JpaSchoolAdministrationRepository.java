package uz.academixai.school.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.School;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SchoolEntity;
import uz.academixai.infrastructure.persistence.SchoolRepository;
import uz.academixai.school.application.port.out.SchoolAdministrationRepository;

/** Transitional JPA adapter over the existing schema while School entities are migrated. */
@Repository
public class JpaSchoolAdministrationRepository implements SchoolAdministrationRepository {

  private final SchoolRepository schoolRepository;
  private final SchoolClassRepository classRepository;

  public JpaSchoolAdministrationRepository(
      SchoolRepository schoolRepository, SchoolClassRepository classRepository) {
    this.schoolRepository = schoolRepository;
    this.classRepository = classRepository;
  }

  @Override
  public Optional<School> findById(UUID schoolId) {
    return schoolRepository.findById(schoolId).map(SchoolEntity::toDomain);
  }

  @Override
  public long countActiveClasses(UUID schoolId) {
    return classRepository.countBySchoolIdAndIsActiveTrue(schoolId);
  }

  @Override
  public School save(School school) {
    return schoolRepository.save(SchoolEntity.fromDomain(school)).toDomain();
  }
}
