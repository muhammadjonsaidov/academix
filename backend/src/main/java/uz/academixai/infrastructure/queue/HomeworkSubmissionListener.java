package uz.academixai.infrastructure.queue;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uz.academixai.intelligence.application.HomeworkAiAnalysisService;
import uz.academixai.shared.tenancy.TenantScope;

/**
 * Consumes {@code homework.submissions.queue} — see {@link HomeworkQueueConfig} for retry/DLX/TTL
 * config and {@link HomeworkAiAnalysisService} for the actual OCR+grading pipeline. A thrown
 * exception here triggers the container's retry advice (3 attempts, then dead-lettered) — {@link
 * HomeworkAiAnalysisService} deliberately does NOT let a Qwen/Vision outage reach this point (it
 * degrades to {@code AI_SKIPPED} internally instead), so only genuinely unexpected failures (e.g. a
 * missing submission row) end up retried/dead-lettered here.
 *
 * <p><b>Runs in the publishing tenant's scope.</b> This listener has no HTTP request to derive
 * {@code schoolId} from, so {@link SubmissionQueueMessage} carries it, and the work runs through
 * {@link TenantScope#runAsTenant} — the same mechanism {@code RlsTransactionFilter} uses for
 * requests. Skipping it is not a subtle degradation: it was confirmed by a real {@code invalid
 * input syntax for type uuid: ""} failure the first time this ran against an RLS-scoped submission.
 */
@Component
public class HomeworkSubmissionListener {

  private final HomeworkAiAnalysisService aiAnalysisService;
  private final TenantScope tenantScope;

  public HomeworkSubmissionListener(
      HomeworkAiAnalysisService aiAnalysisService, TenantScope tenantScope) {
    this.aiAnalysisService = aiAnalysisService;
    this.tenantScope = tenantScope;
  }

  @RabbitListener(queues = HomeworkQueueConfig.SUBMISSIONS_QUEUE)
  public void onSubmission(SubmissionQueueMessage message) {
    tenantScope.runAsTenant(
        message.schoolId(), null, () -> aiAnalysisService.analyze(message.submissionId()));
  }
}
