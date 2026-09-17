package uz.academixai.learning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.Exam;
import uz.academixai.infrastructure.persistence.ExamEntity;
import uz.academixai.infrastructure.persistence.ExamRepository;
import uz.academixai.learning.application.port.out.ExamStore;

/** Transitional JPA adapter over the existing exams schema. */
@Repository
public class JpaExamStore implements ExamStore {

  private final ExamRepository repository;

  public JpaExamStore(ExamRepository repository) {
    this.repository = repository;
  }

  @Override
  public Exam save(Exam exam) {
    return repository.save(ExamEntity.fromDomain(exam)).toDomain();
  }

  @Override
  public List<Exam> findBySchoolIdAndTeacherId(UUID schoolId, UUID teacherId) {
    return repository.findBySchoolIdAndTeacherIdOrderByExamDateDesc(schoolId, teacherId).stream()
        .map(ExamEntity::toDomain)
        .toList();
  }

  @Override
  public List<Exam> findBySchoolIdAndClassId(UUID schoolId, UUID classId) {
    return repository.findBySchoolIdAndClassIdOrderByExamDateDesc(schoolId, classId).stream()
        .map(ExamEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<Exam> findByIdAndSchoolId(UUID examId, UUID schoolId) {
    return repository.findByIdAndSchoolId(examId, schoolId).map(ExamEntity::toDomain);
  }
}
