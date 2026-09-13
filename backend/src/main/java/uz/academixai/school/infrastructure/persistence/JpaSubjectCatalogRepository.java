package uz.academixai.school.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.Subject;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.school.application.port.out.SubjectCatalogRepository;

/** Transitional JPA adapter over the existing subject schema. */
@Repository
public class JpaSubjectCatalogRepository implements SubjectCatalogRepository {

  private final SubjectRepository subjectRepository;

  public JpaSubjectCatalogRepository(SubjectRepository subjectRepository) {
    this.subjectRepository = subjectRepository;
  }

  @Override
  public List<Subject> findBySchoolId(UUID schoolId) {
    return subjectRepository.findBySchoolIdOrderByName(schoolId).stream()
        .map(SubjectEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<Subject> findByIdAndSchoolId(UUID subjectId, UUID schoolId) {
    return subjectRepository.findByIdAndSchoolId(subjectId, schoolId).map(SubjectEntity::toDomain);
  }

  @Override
  public boolean existsBySchoolIdAndNameIgnoringCase(UUID schoolId, String name) {
    return subjectRepository.findBySchoolIdOrderByName(schoolId).stream()
        .anyMatch(subject -> subject.getName().equalsIgnoreCase(name));
  }

  @Override
  public boolean isReferenced(UUID subjectId) {
    return subjectRepository.isReferenced(subjectId);
  }

  @Override
  public Subject save(Subject subject) {
    return subjectRepository.save(SubjectEntity.fromDomain(subject)).toDomain();
  }

  @Override
  public void deleteById(UUID subjectId) {
    subjectRepository.deleteById(subjectId);
  }
}
