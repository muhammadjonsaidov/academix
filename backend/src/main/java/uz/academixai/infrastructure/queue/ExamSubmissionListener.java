package uz.academixai.infrastructure.queue;

import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.intelligence.application.ExamAiAnalysisService;

/**
 * Consumes {@code exam.submissions.queue} — same {@code SET LOCAL app.current_school_id} pattern as
 * {@link HomeworkSubmissionListener} ({@code exam_submissions}/{@code exam_ai_feedbacks} are both
 * RLS-enabled, and this listener has no HTTP request to derive {@code schoolId} from).
 */
@Component
public class ExamSubmissionListener {

  private final ExamAiAnalysisService examAIAnalysisService;
  private final TransactionTemplate transactionTemplate;
  private final EntityManager entityManager;

  public ExamSubmissionListener(
      ExamAiAnalysisService examAIAnalysisService,
      TransactionTemplate transactionTemplate,
      EntityManager entityManager) {
    this.examAIAnalysisService = examAIAnalysisService;
    this.transactionTemplate = transactionTemplate;
    this.entityManager = entityManager;
  }

  @RabbitListener(queues = ExamQueueConfig.SUBMISSIONS_QUEUE)
  public void onSubmission(SubmissionQueueMessage message) {
    transactionTemplate.executeWithoutResult(
        status -> {
          entityManager
              .createNativeQuery("SET LOCAL app.current_school_id = '" + message.schoolId() + "'")
              .executeUpdate();
          examAIAnalysisService.analyze(message.submissionId());
        });
  }
}
