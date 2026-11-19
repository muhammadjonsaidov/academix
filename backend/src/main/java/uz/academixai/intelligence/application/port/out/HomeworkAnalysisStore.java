package uz.academixai.intelligence.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.intelligence.domain.GradingCriterion;
import uz.academixai.shared.ai.GradingMode;

/** Learning read/write boundary used by the asynchronous homework analysis process. */
public interface HomeworkAnalysisStore {

  record AssignmentContext(
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String subjectName,
      Integer grade,
      GradingMode mode,
      List<GradingCriterion> criteria) {}

  boolean claimForAnalysis(UUID submissionId);

  Optional<HomeworkSubmission> findSubmission(UUID submissionId);

  Optional<AssignmentContext> findAssignmentContext(UUID assignmentId);

  void saveFeedback(AIFeedback feedback);

  void saveSubmission(HomeworkSubmission submission);

  void recordAiUsage(UUID schoolId, AssignmentContext context);
}
