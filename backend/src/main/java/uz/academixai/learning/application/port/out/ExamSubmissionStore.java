package uz.academixai.learning.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.ExamSubmission;

/** Persistence boundary for the Learning exam-submission aggregate. */
public interface ExamSubmissionStore {

  ExamSubmission save(ExamSubmission submission);

  List<ExamSubmission> findByExamId(UUID examId);

  Optional<ExamSubmission> findLatestByExamIdAndStudentId(UUID examId, UUID studentId);

  Optional<ExamSubmission> findByIdAndExamId(UUID submissionId, UUID examId);
}
