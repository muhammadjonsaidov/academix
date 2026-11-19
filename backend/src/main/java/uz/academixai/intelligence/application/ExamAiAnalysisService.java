package uz.academixai.intelligence.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.intelligence.application.port.out.AnalysisStatusNotifier;
import uz.academixai.intelligence.application.port.out.ExamAnalysisStore;
import uz.academixai.intelligence.application.port.out.GradingAi;
import uz.academixai.intelligence.application.port.out.OcrGateway;
import uz.academixai.intelligence.application.port.out.SubmissionImageStore;
import uz.academixai.intelligence.domain.AiCallCategory;
import uz.academixai.intelligence.domain.GradingAnalysis;
import uz.academixai.intelligence.domain.GradingModes;

/** Intelligence-owned asynchronous OCR and grading process for scanned exam papers. */
@Service
public class ExamAiAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(ExamAiAnalysisService.class);
  private static final int MIN_MEANINGFUL_TEXT_LENGTH = 20;

  private final ExamAnalysisStore submissions;
  private final SubmissionImageStore images;
  private final OcrGateway ocr;
  private final GradingAi grading;
  private final AiBudgetService budget;
  private final AnalysisStatusNotifier notifications;

  public ExamAiAnalysisService(
      ExamAnalysisStore submissions,
      SubmissionImageStore images,
      OcrGateway ocr,
      GradingAi grading,
      AiBudgetService budget,
      AnalysisStatusNotifier notifications) {
    this.submissions = submissions;
    this.images = images;
    this.ocr = ocr;
    this.grading = grading;
    this.budget = budget;
    this.notifications = notifications;
  }

  public void analyze(UUID submissionId) {
    if (!submissions.claimForAnalysis(submissionId)) {
      return;
    }
    ExamSubmission submission =
        submissions
            .findSubmission(submissionId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Queue message for unknown examSubmissionId " + submissionId));

    String extractedText;
    try {
      extractedText = ocr.extract(images.download(submission.imageUrl()));
    } catch (OcrUnavailableException exception) {
      saveOcrOnlyFeedback(submission, "");
      updateStatus(submission, SubmissionStatus.AI_SKIPPED, true);
      return;
    }

    boolean flagged = isFlagged(extractedText);
    if (extractedText == null || extractedText.trim().isEmpty()) {
      saveEmptyFeedback(submission);
      updateStatus(submission, SubmissionStatus.AI_DONE, true);
      return;
    }
    if (!budget.isWithinAiBudget(submission.schoolId(), AiCallCategory.EXAM)) {
      saveOcrOnlyFeedback(submission, extractedText);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED, true);
      return;
    }

    ExamAnalysisStore.ExamContext context =
        submissions
            .findExamContext(submission.examId())
            .orElseThrow(
                () -> new IllegalStateException("Exam missing for submission " + submission.id()));
    GradingAnalysis result;
    try {
      result =
          grading.grade(
              context.mode(),
              subjectAndGrade(context),
              context.criteria().isEmpty()
                  ? GradingModes.defaultCriteria(context.mode())
                  : context.criteria(),
              extractedText);
    } catch (AiProviderUnavailableException exception) {
      log.warn(
          "AI provider grading unavailable for exam submission {}, falling to AI_SKIPPED",
          submission.id(),
          exception);
      saveOcrOnlyFeedback(submission, extractedText);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED, true);
      return;
    }

    budget.recordAiUsage(submission.schoolId(), AiCallCategory.EXAM);
    submissions.recordAiUsage(submission.schoolId(), context);
    float score = weightedSum(result.criteriaScores());
    saveGradedFeedback(submission, extractedText, result, score);
    updateStatus(submission, SubmissionStatus.AI_DONE, flagged);
  }

  private void saveEmptyFeedback(ExamSubmission submission) {
    submissions.saveFeedback(
        new ExamAIFeedback(
            UUID.randomUUID(),
            submission.schoolId(),
            submission.id(),
            "",
            List.of(),
            List.of(),
            0f,
            "Topshiriq bo'sh yoki juda qisqa — baholab bo'lmadi.",
            LocalDateTime.now()));
  }

  private void saveOcrOnlyFeedback(ExamSubmission submission, String extractedText) {
    submissions.saveFeedback(
        new ExamAIFeedback(
            UUID.randomUUID(),
            submission.schoolId(),
            submission.id(),
            extractedText,
            List.of(),
            List.of(),
            0f,
            "AI tahlil vaqtincha ishlamadi — o'qituvchi qo'lda baholaydi.",
            LocalDateTime.now()));
  }

  private void saveGradedFeedback(
      ExamSubmission submission, String extractedText, GradingAnalysis result, float score) {
    submissions.saveFeedback(
        new ExamAIFeedback(
            UUID.randomUUID(),
            submission.schoolId(),
            submission.id(),
            extractedText,
            result.stepAnalyses(),
            result.criteriaScores(),
            score,
            result.feedback(),
            LocalDateTime.now()));
  }

  private void updateStatus(
      ExamSubmission submission, SubmissionStatus status, boolean flaggedForReview) {
    submissions.saveSubmission(
        new ExamSubmission(
            submission.id(),
            submission.schoolId(),
            submission.examId(),
            submission.studentId(),
            submission.imageUrl(),
            status,
            flaggedForReview,
            submission.uploadedAt()));
    submissions
        .findExamContext(submission.examId())
        .ifPresent(
            context ->
                notifications.notifyExam(
                    context.teacherId(), submission.studentId(), submission.id(), status));
  }

  private static boolean isFlagged(String text) {
    return text == null || text.length() < MIN_MEANINGFUL_TEXT_LENGTH;
  }

  private static String subjectAndGrade(ExamAnalysisStore.ExamContext context) {
    return context.grade() == null
        ? context.subjectName()
        : context.subjectName() + " " + context.grade() + "-sinf";
  }

  private static float weightedSum(List<CriteriaScore> scores) {
    double sum = 0;
    for (CriteriaScore score : scores) {
      sum += score.score() * score.weightPercent();
    }
    return (float) (sum / 100.0);
  }
}
