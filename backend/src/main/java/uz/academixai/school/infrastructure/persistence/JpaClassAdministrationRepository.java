package uz.academixai.school.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.SchoolClass;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.school.application.port.out.ClassAdministrationRepository;

/** Transitional JPA adapter over the existing class schema while School entities are migrated. */
@Repository
public class JpaClassAdministrationRepository implements ClassAdministrationRepository {

  private final SchoolClassRepository classRepository;

  public JpaClassAdministrationRepository(SchoolClassRepository classRepository) {
    this.classRepository = classRepository;
  }

  @Override
  public List<SchoolClass> findBySchoolId(UUID schoolId) {
    return classRepository.findBySchoolIdOrderByGradeAscLetterAsc(schoolId).stream()
        .map(SchoolClassEntity::toDomain)
        .toList();
  }

  @Override
  public boolean existsBySchoolIdAndGradeAndLetter(UUID schoolId, int grade, String letter) {
    return classRepository.existsBySchoolIdAndGradeAndLetter(schoolId, grade, letter);
  }

  @Override
  public Optional<SchoolClass> findByIdAndSchoolId(UUID classId, UUID schoolId) {
    return classRepository.findByIdAndSchoolId(classId, schoolId).map(SchoolClassEntity::toDomain);
  }

  @Override
  public SchoolClass save(SchoolClass schoolClass) {
    return classRepository.save(SchoolClassEntity.fromDomain(schoolClass)).toDomain();
  }

  @Override
  public void deleteById(UUID classId) {
    classRepository.deleteById(classId);
  }
}
