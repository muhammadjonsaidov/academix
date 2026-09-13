package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.ExamGrade;

/** Persistence boundary for final teacher grades of exam submissions. */
public interface ExamGradeStore {

  Optional<ExamGrade> findBySubmissionId(UUID submissionId);

  ExamGrade save(ExamGrade grade);
}
