package uz.academixai.infrastructure.queue;

import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import uz.academixai.application.AIAnalysisService;

/**
 * Consumes {@code homework.submissions.queue} — see {@link HomeworkQueueConfig} for retry/DLX/TTL
 * config and {@link AIAnalysisService} for the actual OCR+grading pipeline. A thrown exception here
 * triggers the container's retry advice (3 attempts, then dead-lettered) — {@link
 * AIAnalysisService} deliberately does NOT let a Qwen/Vision outage reach this point (it degrades
 * to {@code AI_SKIPPED} internally instead), so only genuinely unexpected failures (e.g. a missing
 * submission row) end up retried/dead-lettered here.
 *
 * <p><b>Runs its own {@code SET LOCAL app.current_school_id}, same as {@code RlsTransactionFilter}
 * does for HTTP requests.</b> {@code homework_submissions} is RLS-enabled, but this listener has no
 * HTTP request to derive {@code schoolId} from — confirmed necessary by a real {@code invalid input
 * syntax for type uuid: ""} failure the first time this ran against a real submission, the exact
 * "no session variable set" failure mode documented in {@code RlsMechanismTest}. {@link
 * SubmissionQueueMessage} carries {@code schoolId} for exactly this reason.
 */
@Component
public class HomeworkSubmissionListener {

  private final AIAnalysisService aiAnalysisService;
  private final TransactionTemplate transactionTemplate;
  private final EntityManager entityManager;

  public HomeworkSubmissionListener(
      AIAnalysisService aiAnalysisService,
      TransactionTemplate transactionTemplate,
      EntityManager entityManager) {
    this.aiAnalysisService = aiAnalysisService;
    this.transactionTemplate = transactionTemplate;
    this.entityManager = entityManager;
  }

  @RabbitListener(queues = HomeworkQueueConfig.SUBMISSIONS_QUEUE)
  public void onSubmission(SubmissionQueueMessage message) {
    transactionTemplate.executeWithoutResult(
        status -> {
          // Same reasoning as RlsTransactionFilter: SET LOCAL doesn't accept JDBC bind
          // parameters, so the UUID is inlined directly — safe here because schoolId always
          // originated from a validated JWT claim at publish time, never raw user input.
          entityManager
              .createNativeQuery("SET LOCAL app.current_school_id = '" + message.schoolId() + "'")
              .executeUpdate();
          aiAnalysisService.analyzeSubmission(message.submissionId());
        });
  }
}
