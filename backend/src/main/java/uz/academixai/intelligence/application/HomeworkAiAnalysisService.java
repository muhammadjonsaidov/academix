package uz.academixai.intelligence.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.HandwritingCheckResult;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.intelligence.application.port.out.AnalysisStatusNotifier;
import uz.academixai.intelligence.application.port.out.GradingAi;
import uz.academixai.intelligence.application.port.out.HandwritingAnalyzer;
import uz.academixai.intelligence.application.port.out.HomeworkAnalysisStore;
import uz.academixai.intelligence.application.port.out.OcrGateway;
import uz.academixai.intelligence.application.port.out.ProgressAchievementAwarder;
import uz.academixai.intelligence.application.port.out.SubmissionImageStore;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.intelligence.domain.GradingAnalysis;
import uz.academixai.intelligence.domain.GradingCriterion;
import uz.academixai.intelligence.domain.OcrDocument;

/**
 * Intelligence-owned asynchronous homework OCR, grading and plagiarism analysis process.
 *
 * <p>It accepts a Learning submission id from the durable queue, claims it atomically, then records
 * only AI feedback and the analysis status. A teacher remains the owner of the final grade.
 */
@Service
public class HomeworkAiAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(HomeworkAiAnalysisService.class);
  private static final int MIN_MEANINGFUL_TEXT_LENGTH = 5;
  private static final List<GradingCriterion> DEFAULT_CRITERIA =
      List.of(
          new GradingCriterion("Yechish usuli", 40),
          new GradingCriterion("Javob to'g'riligi", 30),
          new GradingCriterion("Tushunarlilik", 30));

  private final HomeworkAnalysisStore submissions;
  private final SubmissionImageStore images;
  private final OcrGateway ocr;
  private final HandwritingAnalyzer handwriting;
  private final GradingAi grading;
  private final AiBudgetService budget;
  private final ProgressAchievementAwarder achievements;
  private final AnalysisStatusNotifier notifications;

  public HomeworkAiAnalysisService(
      HomeworkAnalysisStore submissions,
      SubmissionImageStore images,
      OcrGateway ocr,
      HandwritingAnalyzer handwriting,
      GradingAi grading,
      AiBudgetService budget,
      ProgressAchievementAwarder achievements,
      AnalysisStatusNotifier notifications) {
    this.submissions = submissions;
    this.images = images;
    this.ocr = ocr;
    this.handwriting = handwriting;
    this.grading = grading;
    this.budget = budget;
    this.achievements = achievements;
    this.notifications = notifications;
  }

  public void analyze(UUID submissionId) {
    if (!submissions.claimForAnalysis(submissionId)) {
      return;
    }
    HomeworkSubmission submission =
        submissions
            .findSubmission(submissionId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Queue message for unknown submissionId " + submissionId));

    OcrDocument document;
    try {
      document = extractText(submission);
    } catch (OcrUnavailableException exception) {
      saveOcrOnlyFeedback(submission, "", null);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }

    HandwritingCheckResult handwritingResult =
        document.characters().isEmpty()
            ? null
            : handwriting.checkAndUpdate(submission.studentId(), document);
    String extractedText = document.extractedText();
    if (extractedText == null || extractedText.trim().length() < MIN_MEANINGFUL_TEXT_LENGTH) {
      saveEmptyFeedback(submission, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_DONE);
      achievements.applyAiScore(submission.id(), submission.studentId(), 0f, submission.isLate());
      return;
    }

    if (!budget.isWithinAiBudget(submission.schoolId(), AiCallCategory.HOMEWORK)) {
      saveOcrOnlyFeedback(submission, extractedText, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }

    HomeworkAnalysisStore.AssignmentContext context =
        submissions
            .findAssignmentContext(submission.assignmentId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Assignment missing for submission " + submission.id()));
    GradingAnalysis result;
    try {
      result =
          grading.grade(
              subjectAndGrade(context),
              context.criteria().isEmpty() ? DEFAULT_CRITERIA : context.criteria(),
              extractedText);
    } catch (AiProviderUnavailableException exception) {
      log.warn(
          "AI provider grading unavailable for submission {}, falling to AI_SKIPPED",
          submission.id(),
          exception);
      saveOcrOnlyFeedback(submission, extractedText, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }

    budget.recordAiUsage(submission.schoolId(), AiCallCategory.HOMEWORK);
    submissions.recordAiUsage(submission.schoolId(), context);
    float aiScore = weightedSum(result.criteriaScores());
    saveGradedFeedback(submission, extractedText, result, aiScore, handwritingResult);
    updateStatus(submission, SubmissionStatus.AI_DONE);
    achievements.applyAiScore(
        submission.id(), submission.studentId(), aiScore, submission.isLate());
  }

  private OcrDocument extractText(HomeworkSubmission submission) {
    boolean hasImage = submission.imageUrl() != null && !submission.imageUrl().isBlank();
    if (submission.type() == SubmissionType.TEXT || !hasImage) {
      return new OcrDocument(submission.textContent(), List.of());
    }
    OcrDocument document = ocr.extract(images.download(submission.imageUrl()));
    if (submission.type() == SubmissionType.MIXED
        && submission.textContent() != null
        && !submission.textContent().isBlank()) {
      return new OcrDocument(
          submission.textContent() + "\n" + document.extractedText(), document.characters());
    }
    return document;
  }

  private void saveEmptyFeedback(
      HomeworkSubmission submission, HandwritingCheckResult handwriting) {
    submissions.saveFeedback(
        new AIFeedback(
            UUID.randomUUID(),
            submission.id(),
            "",
            0f,
            List.of(),
            List.of(),
            0f,
            "Topshiriq bo'sh yoki juda qisqa — baholab bo'lmadi.",
            null,
            0f,
            resolvePlagiarismType(PlagiarismType.CLEAN, handwriting),
            handwritingScore(handwriting),
            LocalDateTime.now()));
  }

  private void saveOcrOnlyFeedback(
      HomeworkSubmission submission, String extractedText, HandwritingCheckResult handwriting) {
    submissions.saveFeedback(
        new AIFeedback(
            UUID.randomUUID(),
            submission.id(),
            extractedText,
            0f,
            List.of(),
            List.of(),
            0f,
            "AI tahlil vaqtincha ishlamadi — o'qituvchi qo'lda baholaydi.",
            null,
            0f,
            resolvePlagiarismType(PlagiarismType.CLEAN, handwriting),
            handwritingScore(handwriting),
            LocalDateTime.now()));
  }

  private void saveGradedFeedback(
      HomeworkSubmission submission,
      String extractedText,
      GradingAnalysis result,
      float score,
      HandwritingCheckResult handwriting) {
    submissions.saveFeedback(
        new AIFeedback(
            UUID.randomUUID(),
            submission.id(),
            extractedText,
            0f,
            result.stepAnalyses(),
            result.criteriaScores(),
            score,
            result.feedback(),
            null,
            (float) result.plagiarismScore(),
            resolvePlagiarismType(parsePlagiarismType(result.plagiarismType()), handwriting),
            handwritingScore(handwriting),
            LocalDateTime.now()));
  }

  private void updateStatus(HomeworkSubmission submission, SubmissionStatus status) {
    submissions.saveSubmission(
        new HomeworkSubmission(
            submission.id(),
            submission.schoolId(),
            submission.assignmentId(),
            submission.studentTaskId(),
            submission.studentId(),
            submission.type(),
            submission.textContent(),
            submission.imageUrl(),
            status,
            submission.isLate(),
            submission.submittedAt(),
            submission.xpEarned()));
    submissions
        .findAssignmentContext(submission.assignmentId())
        .ifPresent(
            context ->
                notifications.notifyHomework(
                    context.teacherId(), submission.studentId(), submission.id(), status));
  }

  private static String subjectAndGrade(HomeworkAnalysisStore.AssignmentContext context) {
    return context.grade() == null
        ? context.subjectName()
        : context.subjectName() + " " + context.grade() + "-sinf";
  }

  private static float weightedSum(List<CriteriaScore> criteriaScores) {
    double sum = 0;
    for (CriteriaScore score : criteriaScores) {
      sum += score.score() * score.weightPercent();
    }
    return (float) (sum / 100.0);
  }

  private static float handwritingScore(HandwritingCheckResult handwriting) {
    return handwriting == null ? 0f : handwriting.matchScore();
  }

  private static PlagiarismType resolvePlagiarismType(
      PlagiarismType qwenType, HandwritingCheckResult handwriting) {
    return handwriting != null && handwriting.type() == PlagiarismType.HANDWRITING_MISMATCH
        ? PlagiarismType.HANDWRITING_MISMATCH
        : qwenType;
  }

  private static PlagiarismType parsePlagiarismType(String raw) {
    try {
      return PlagiarismType.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return PlagiarismType.CLEAN;
    }
  }
}
