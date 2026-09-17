package uz.academixai.learning.application.port.out;

import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.Grade;

/** Feedback and teacher-grade read boundary for a submission detail view. */
public interface SubmissionAssessmentLookup {

  Optional<AIFeedback> feedback(UUID submissionId);

  Optional<Grade> grade(UUID submissionId);
}
