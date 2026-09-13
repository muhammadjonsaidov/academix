package uz.academixai.intelligence.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.intelligence.domain.GradingCriterion;

/** Learning exam read/write boundary used by the asynchronous Intelligence analysis process. */
public interface ExamAnalysisStore {

  record ExamContext(
      UUID teacherId,
      UUID classId,
      UUID subjectId,
      String subjectName,
      Integer grade,
      List<GradingCriterion> criteria) {}

  boolean claimForAnalysis(UUID submissionId);

  Optional<ExamSubmission> findSubmission(UUID submissionId);

  Optional<ExamContext> findExamContext(UUID examId);

  void saveFeedback(ExamAIFeedback feedback);

  void saveSubmission(ExamSubmission submission);

  void recordAiUsage(UUID schoolId, ExamContext context);
}
