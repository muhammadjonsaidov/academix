package uz.academixai.intelligence.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.intelligence.application.port.out.AiMonthlyLimitLookup;
import uz.academixai.intelligence.application.port.out.AiUsageCounter;
import uz.academixai.intelligence.application.port.out.AnalysisStatusNotifier;
import uz.academixai.intelligence.application.port.out.ExamAnalysisStore;
import uz.academixai.intelligence.application.port.out.GradingAi;
import uz.academixai.intelligence.application.port.out.HandwritingAnalyzer;
import uz.academixai.intelligence.application.port.out.OcrGateway;
import uz.academixai.intelligence.application.port.out.SubmissionImageStore;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.intelligence.domain.OcrDocument;

class ExamAiAnalysisServiceTest {

  @Test
  void emptyOcrMarksExamForReviewAndCompletesAnalysis() {
    UUID schoolId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();
    Store store =
        new Store(
            new ExamSubmission(
                submissionId,
                schoolId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "exam-image",
                SubmissionStatus.SUBMITTED,
                false,
                LocalDateTime.now()));

    service(store).analyze(submissionId);

    assertThat(store.feedback)
        .singleElement()
        .extracting(ExamAIFeedback::aiScorePercent)
        .isEqualTo(0f);
    assertThat(store.saved)
        .singleElement()
        .satisfies(
            saved -> {
              assertThat(saved.status()).isEqualTo(SubmissionStatus.AI_DONE);
              assertThat(saved.flaggedForReview()).isTrue();
            });
  }

  private static ExamAiAnalysisService service(Store store) {
    AiMonthlyLimitLookup limits = schoolId -> Optional.of(100);
    AiUsageCounter counter =
        new AiUsageCounter() {
          @Override
          public long currentUsage(UUID schoolId, AiCallCategory category) {
            return 0;
          }

          @Override
          public void increment(UUID schoolId, AiCallCategory category) {}
        };
    SubmissionImageStore images = key -> new byte[0];
    OcrGateway ocr = image -> new OcrDocument("", List.of());
    HandwritingAnalyzer handwriting = (studentId, document) -> null;
    GradingAi grading = (subject, criteria, text) -> null;
    AnalysisStatusNotifier notifier =
        new AnalysisStatusNotifier() {
          @Override
          public void notifyHomework(
              UUID teacherId, UUID studentId, UUID submissionId, SubmissionStatus status) {}

          @Override
          public void notifyExam(
              UUID teacherId, UUID studentId, UUID submissionId, SubmissionStatus status) {}
        };
    return new ExamAiAnalysisService(
        store, images, ocr, handwriting, grading, new AiBudgetService(limits, counter), notifier);
  }

  private static final class Store implements ExamAnalysisStore {

    private final ExamSubmission submission;
    private final List<ExamAIFeedback> feedback = new ArrayList<>();
    private final List<ExamSubmission> saved = new ArrayList<>();

    private Store(ExamSubmission submission) {
      this.submission = submission;
    }

    @Override
    public boolean claimForAnalysis(UUID submissionId) {
      return true;
    }

    @Override
    public Optional<ExamSubmission> findSubmission(UUID submissionId) {
      return Optional.of(submission);
    }

    @Override
    public Optional<ExamContext> findExamContext(UUID examId) {
      return Optional.empty();
    }

    @Override
    public void saveFeedback(ExamAIFeedback value) {
      feedback.add(value);
    }

    @Override
    public void saveSubmission(ExamSubmission value) {
      saved.add(value);
    }

    @Override
    public void recordAiUsage(UUID schoolId, ExamContext context) {}
  }
}
