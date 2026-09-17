package uz.academixai.learning.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.learning.application.port.out.ExamSubmissionStore;

/** JPA adapter for Learning's exam-submission aggregate. */
@Repository
public class JpaExamSubmissionStore implements ExamSubmissionStore {

  private final ExamSubmissionRepository repository;

  public JpaExamSubmissionStore(ExamSubmissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public ExamSubmission save(ExamSubmission submission) {
    return repository.save(ExamSubmissionEntity.fromDomain(submission)).toDomain();
  }

  @Override
  public List<ExamSubmission> findByExamId(UUID examId) {
    return repository.findByExamIdOrderByUploadedAtDesc(examId).stream()
        .map(ExamSubmissionEntity::toDomain)
        .toList();
  }

  @Override
  public Optional<ExamSubmission> findLatestByExamIdAndStudentId(UUID examId, UUID studentId) {
    return repository
        .findFirstByExamIdAndStudentIdOrderByUploadedAtDesc(examId, studentId)
        .map(ExamSubmissionEntity::toDomain);
  }

  @Override
  public Optional<ExamSubmission> findByIdAndExamId(UUID submissionId, UUID examId) {
    return repository.findByIdAndExamId(submissionId, examId).map(ExamSubmissionEntity::toDomain);
  }
}
