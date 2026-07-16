package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.ExamAIFeedback;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.HandwritingCheckResult;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.ai.AiBudgetService;
import uz.academixai.infrastructure.ai.AiCallCategory;
import uz.academixai.infrastructure.ai.GoogleVisionClient;
import uz.academixai.infrastructure.ai.GradingCriterion;
import uz.academixai.infrastructure.ai.OcrUnavailableException;
import uz.academixai.infrastructure.ai.QwenAIClient;
import uz.academixai.infrastructure.ai.QwenGradingResult;
import uz.academixai.infrastructure.ai.QwenUnavailableException;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackEntity;
import uz.academixai.infrastructure.persistence.ExamAIFeedbackRepository;
import uz.academixai.infrastructure.persistence.ExamEntity;
import uz.academixai.infrastructure.persistence.ExamRepository;
import uz.academixai.infrastructure.persistence.ExamSubmissionEntity;
import uz.academixai.infrastructure.persistence.ExamSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.storage.FileStorageService;

/**
 * academix_backend_tdd.md §6.7 — {@code exam.submissions.queue} consumer's actual pipeline. Same
 * OCR+grading shape as {@link AIAnalysisService}, deliberately separate service (not a shared
 * method) because the two diverge in enough places — no plagiarism check (exams are proctored on
 * paper), no XP/streak, exam-only {@code flaggedForReview} threshold, dedicated {@code EXAM} budget
 * — that forcing one shared method would need more branching than it saves.
 *
 * <p><b>Reuses the same combined grading+plagiarism Qwen call</b> ({@link
 * QwenAIClient#gradeSubmission}), not a separate "grade-only" prompt — the spec's own pseudocode
 * references a {@code gradeOnly} call but never defines a distinct prompt contract for it (§3 only
 * documents the merged one). Judgment call: reuse the merged call and simply discard {@code
 * plagiarismScore}/{@code plagiarismType} from the response — {@code exam_ai_feedbacks} has no
 * columns for either, so nothing is persisted. No extra cost either way (same one call).
 */
@Service
public class ExamAIAnalysisService {

  // backend_tdd.md §6.7's exact rule.
  private static final float MISMATCH_THRESHOLD = 60.0f;
  private static final int MIN_MEANINGFUL_TEXT_LENGTH = 20;

  private static final List<GradingCriterion> DEFAULT_CRITERIA =
      List.of(
          new GradingCriterion("Yechish usuli", 40),
          new GradingCriterion("Javob to'g'riligi", 30),
          new GradingCriterion("Tushunarlilik", 30));

  private final ExamSubmissionRepository submissionRepository;
  private final ExamRepository examRepository;
  private final SubjectRepository subjectRepository;
  private final SchoolClassRepository classRepository;
  private final ExamAIFeedbackRepository feedbackRepository;
  private final FileStorageService fileStorageService;
  private final GoogleVisionClient googleVisionClient;
  private final QwenAIClient qwenAIClient;
  private final AiBudgetService aiBudgetService;
  private final GradingCriteriaService gradingCriteriaService;
  private final HandwritingService handwritingService;

  public ExamAIAnalysisService(
      ExamSubmissionRepository submissionRepository,
      ExamRepository examRepository,
      SubjectRepository subjectRepository,
      SchoolClassRepository classRepository,
      ExamAIFeedbackRepository feedbackRepository,
      FileStorageService fileStorageService,
      GoogleVisionClient googleVisionClient,
      QwenAIClient qwenAIClient,
      AiBudgetService aiBudgetService,
      GradingCriteriaService gradingCriteriaService,
      HandwritingService handwritingService) {
    this.submissionRepository = submissionRepository;
    this.examRepository = examRepository;
    this.subjectRepository = subjectRepository;
    this.classRepository = classRepository;
    this.feedbackRepository = feedbackRepository;
    this.fileStorageService = fileStorageService;
    this.googleVisionClient = googleVisionClient;
    this.qwenAIClient = qwenAIClient;
    this.aiBudgetService = aiBudgetService;
    this.gradingCriteriaService = gradingCriteriaService;
    this.handwritingService = handwritingService;
  }

  public void analyzeExamSubmission(UUID examSubmissionId) {
    ExamSubmissionEntity subEntity =
        submissionRepository
            .findById(examSubmissionId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Queue message for unknown examSubmissionId " + examSubmissionId));
    ExamSubmission submission = subEntity.toDomain();
    updateStatus(submission, SubmissionStatus.AI_PROCESSING, submission.flaggedForReview());

    String extractedText;
    HandwritingCheckResult handwritingResult;
    try {
      byte[] imageBytes = fileStorageService.download(submission.imageUrl());
      var ocrResult = googleVisionClient.extractText(imageBytes);
      extractedText = ocrResult.extractedText();
      handwritingResult =
          ocrResult.layout() == null
              ? null
              : handwritingService.checkAndUpdateProfile(
                  submission.studentId(), ocrResult.layout());
    } catch (OcrUnavailableException e) {
      saveOcrOnlyFeedback(submission, "", null);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED, true);
      return;
    }

    boolean flaggedForReview = isFlagged(extractedText, handwritingResult);

    if (extractedText == null || extractedText.trim().isEmpty()) {
      saveEmptyFeedback(submission, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_DONE, true);
      return;
    }

    if (!aiBudgetService.isWithinAiBudget(submission.schoolId(), AiCallCategory.EXAM)) {
      saveOcrOnlyFeedback(submission, extractedText, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED, true);
      return;
    }

    ExamEntity exam =
        examRepository
            .findById(submission.examId())
            .orElseThrow(() -> new IllegalStateException("Exam missing for submission"));
    String subjectAndGrade = subjectAndGrade(exam);
    List<GradingCriterion> criteria = resolveCriteria(exam);

    QwenGradingResult result;
    try {
      result = qwenAIClient.gradeSubmission(subjectAndGrade, criteria, extractedText);
    } catch (QwenUnavailableException e) {
      saveOcrOnlyFeedback(submission, extractedText, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED, true);
      return;
    }
    aiBudgetService.recordAiUsage(submission.schoolId(), AiCallCategory.EXAM);

    float aiScorePercent = weightedSum(result.criteriaScores());
    saveGradedFeedback(submission, extractedText, result, aiScorePercent, handwritingResult);
    updateStatus(submission, SubmissionStatus.AI_DONE, flaggedForReview);
  }

  /** backend_tdd.md §6.7's exact rule: low handwriting match or too-short OCR text. */
  private static boolean isFlagged(String extractedText, HandwritingCheckResult handwriting) {
    boolean tooShort = extractedText == null || extractedText.length() < MIN_MEANINGFUL_TEXT_LENGTH;
    boolean lowMatch = handwriting != null && handwriting.matchScore() < MISMATCH_THRESHOLD;
    return tooShort || lowMatch;
  }

  private List<GradingCriterion> resolveCriteria(ExamEntity exam) {
    List<GradingCriterion> configured =
        gradingCriteriaService.getForGrading(exam.getTeacherId(), exam.toDomain().subjectId());
    return configured.isEmpty() ? DEFAULT_CRITERIA : configured;
  }

  private String subjectAndGrade(ExamEntity exam) {
    String subjectName =
        subjectRepository
            .findById(exam.toDomain().subjectId())
            .map(SubjectEntity::getName)
            .orElse("Fan");
    Integer grade =
        classRepository.findById(exam.getClassId()).map(SchoolClassEntity::getGrade).orElse(null);
    return grade == null ? subjectName : subjectName + " " + grade + "-sinf";
  }

  private static float weightedSum(List<CriteriaScore> criteriaScores) {
    double sum = 0;
    for (CriteriaScore score : criteriaScores) {
      sum += score.score() * score.weightPercent();
    }
    return (float) (sum / 100.0);
  }

  private void saveEmptyFeedback(ExamSubmission submission, HandwritingCheckResult handwriting) {
    ExamAIFeedback feedback =
        new ExamAIFeedback(
            UUID.randomUUID(),
            submission.schoolId(),
            submission.id(),
            "",
            List.of(),
            List.of(),
            0f,
            "Topshiriq bo'sh yoki juda qisqa — baholab bo'lmadi.",
            handwritingScore(handwriting),
            LocalDateTime.now());
    feedbackRepository.save(ExamAIFeedbackEntity.fromDomain(feedback));
  }

  private void saveOcrOnlyFeedback(
      ExamSubmission submission, String extractedText, HandwritingCheckResult handwriting) {
    ExamAIFeedback feedback =
        new ExamAIFeedback(
            UUID.randomUUID(),
            submission.schoolId(),
            submission.id(),
            extractedText,
            List.of(),
            List.of(),
            0f,
            "AI tahlil vaqtincha ishlamadi — o'qituvchi qo'lda baholaydi.",
            handwritingScore(handwriting),
            LocalDateTime.now());
    feedbackRepository.save(ExamAIFeedbackEntity.fromDomain(feedback));
  }

  private void saveGradedFeedback(
      ExamSubmission submission,
      String extractedText,
      QwenGradingResult result,
      float aiScorePercent,
      HandwritingCheckResult handwriting) {
    ExamAIFeedback feedback =
        new ExamAIFeedback(
            UUID.randomUUID(),
            submission.schoolId(),
            submission.id(),
            extractedText,
            result.stepAnalyses(),
            result.criteriaScores(),
            aiScorePercent,
            result.feedback(),
            handwritingScore(handwriting),
            LocalDateTime.now());
    feedbackRepository.save(ExamAIFeedbackEntity.fromDomain(feedback));
  }

  private static float handwritingScore(HandwritingCheckResult handwriting) {
    return handwriting == null ? 0f : handwriting.matchScore();
  }

  private void updateStatus(
      ExamSubmission submission, SubmissionStatus status, boolean flaggedForReview) {
    ExamSubmission updated =
        new ExamSubmission(
            submission.id(),
            submission.schoolId(),
            submission.examId(),
            submission.studentId(),
            submission.imageUrl(),
            status,
            flaggedForReview,
            submission.uploadedAt());
    submissionRepository.save(ExamSubmissionEntity.fromDomain(updated));
  }
}
