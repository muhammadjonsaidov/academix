package uz.academixai.infrastructure.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uz.academixai.intelligence.application.ExamAiAnalysisService;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Consumes {@code exam.submissions.queue} — same tenant-scope mechanism as {@link
 * HomeworkSubmissionListener} ({@code exam_submissions}/{@code exam_ai_feedbacks} are both
 * RLS-enabled, and this listener has no HTTP request to derive {@code schoolId} from).
 */
@Component
public class ExamSubmissionListener {

  private final ExamAiAnalysisService examAIAnalysisService;
  private final TenantScope tenantScope;

  public ExamSubmissionListener(
      ExamAiAnalysisService examAIAnalysisService, TenantScope tenantScope) {
    this.examAIAnalysisService = examAIAnalysisService;
    this.tenantScope = tenantScope;
  }

  @RabbitListener(queues = ExamQueueConfig.SUBMISSIONS_QUEUE)
  public void onSubmission(SubmissionQueueMessage message) {
    tenantScope.runAsTenant(
        message.schoolId(), null, () -> examAIAnalysisService.analyze(message.submissionId()));
  }
}
