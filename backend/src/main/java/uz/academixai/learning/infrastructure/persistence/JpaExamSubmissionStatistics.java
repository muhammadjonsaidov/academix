package uz.academixai.learning.infrastructure.persistence;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.learning.application.port.out.ExamSubmissionStatistics;

/** JPA adapter for counts displayed with Learning exams. */
@Repository
public class JpaExamSubmissionStatistics implements ExamSubmissionStatistics {

  private final ExamSubmissionRepository repository;

  public JpaExamSubmissionStatistics(ExamSubmissionRepository repository) {
    this.repository = repository;
  }

  @Override
  public int countAll(UUID examId) {
    return repository.countByExamId(examId);
  }

  @Override
  public int countGraded(UUID examId) {
    return repository.countByExamIdAndStatus(examId, SubmissionStatus.GRADED);
  }
}
