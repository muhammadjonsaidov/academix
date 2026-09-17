package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.ExamAIFeedback;

/** Read boundary for asynchronous AI assessments of exam papers. */
public interface ExamSubmissionAssessmentLookup {

  Optional<ExamAIFeedback> feedback(UUID submissionId);
}
