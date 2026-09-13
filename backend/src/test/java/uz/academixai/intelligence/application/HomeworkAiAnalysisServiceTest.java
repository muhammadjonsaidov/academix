package uz.academixai.intelligence.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.intelligence.application.port.out.AiMonthlyLimitLookup;
import uz.academixai.intelligence.application.port.out.AiUsageCounter;
import uz.academixai.intelligence.application.port.out.AnalysisStatusNotifier;
import uz.academixai.intelligence.application.port.out.GradingAi;
import uz.academixai.intelligence.application.port.out.HandwritingAnalyzer;
import uz.academixai.intelligence.application.port.out.HomeworkAnalysisStore;
import uz.academixai.intelligence.application.port.out.OcrGateway;
import uz.academixai.intelligence.application.port.out.ProgressAchievementAwarder;
import uz.academixai.intelligence.application.port.out.SubmissionImageStore;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.intelligence.domain.OcrDocument;

class HomeworkAiAnalysisServiceTest {

  @Test
  void shortTextProducesEmptyFeedbackUpdatesStatusAndAwardsZeroScore() {
    UUID schoolId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID submissionId = UUID.randomUUID();
    Store store =
        new Store(
            new HomeworkSubmission(
                submissionId,
                schoolId,
                UUID.randomUUID(),
                null,
                studentId,
                SubmissionType.TEXT,
                "x",
                null,
                SubmissionStatus.SUBMITTED,
                false,
                java.time.LocalDateTime.now(),
                0));
    List<Float> awardedScores = new ArrayList<>();
    HomeworkAiAnalysisService service =
        service(
            store,
            (id, student, score, late) -> awardedScores.add(score),
            (teacher, student, submission, status) -> {});

    service.analyze(submissionId);

    assertThat(store.feedback).singleElement().extracting(AIFeedback::aiScorePercent).isEqualTo(0f);
    assertThat(store.savedSubmissions)
        .singleElement()
        .extracting(HomeworkSubmission::status)
        .isEqualTo(SubmissionStatus.AI_DONE);
    assertThat(awardedScores).containsExactly(0f);
  }

  @Test
  void duplicateDeliveryDoesNotInvokeAnyDownstreamPort() {
    Store store = new Store(null);
    store.claimed = false;
    HomeworkAiAnalysisService service =
        service(
            store, (id, student, score, late) -> {}, (teacher, student, submission, status) -> {});

    service.analyze(UUID.randomUUID());

    assertThat(store.feedback).isEmpty();
    assertThat(store.savedSubmissions).isEmpty();
  }

  private static HomeworkAiAnalysisService service(
      Store store, ProgressAchievementAwarder awards, AnalysisStatusNotifier notifications) {
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
    return new HomeworkAiAnalysisService(
        store,
        images,
        ocr,
        handwriting,
        grading,
        new AiBudgetService(limits, counter),
        awards,
        notifications);
  }

  private static final class Store implements HomeworkAnalysisStore {

    private final HomeworkSubmission submission;
    private boolean claimed = true;
    private final List<AIFeedback> feedback = new ArrayList<>();
    private final List<HomeworkSubmission> savedSubmissions = new ArrayList<>();

    private Store(HomeworkSubmission submission) {
      this.submission = submission;
    }

    @Override
    public boolean claimForAnalysis(UUID submissionId) {
      return claimed;
    }

    @Override
    public Optional<HomeworkSubmission> findSubmission(UUID submissionId) {
      return Optional.ofNullable(submission);
    }

    @Override
    public Optional<AssignmentContext> findAssignmentContext(UUID assignmentId) {
      return Optional.empty();
    }

    @Override
    public void saveFeedback(AIFeedback value) {
      feedback.add(value);
    }

    @Override
    public void saveSubmission(HomeworkSubmission value) {
      savedSubmissions.add(value);
    }

    @Override
    public void recordAiUsage(UUID schoolId, AssignmentContext context) {}
  }
}
