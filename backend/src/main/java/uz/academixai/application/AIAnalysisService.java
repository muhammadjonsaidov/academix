package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.infrastructure.ai.AiBudgetService;
import uz.academixai.infrastructure.ai.AiCallCategory;
import uz.academixai.infrastructure.ai.GoogleVisionClient;
import uz.academixai.infrastructure.ai.GradingCriterion;
import uz.academixai.infrastructure.ai.OcrUnavailableException;
import uz.academixai.infrastructure.ai.QwenAIClient;
import uz.academixai.infrastructure.ai.QwenGradingResult;
import uz.academixai.infrastructure.ai.QwenUnavailableException;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.storage.FileStorageService;

/**
 * academix_backend_tdd.md §6.4 — {@code AIAnalysisService.analyzeSubmission()}, called by the
 * {@code homework.submissions.queue} consumer. OCR (Google Vision) + combined grading/plagiarism
 * (Qwen, one call) with budget-gated graceful degradation.
 *
 * <p><b>Scope note:</b> XP/streak (Sprint 4, §4 XPService) fire on every AI_DONE transition —
 * including the empty/too-short-submission path, since a 0% score still needs to break the streak
 * per TZ §4's XP table ("0% yoki EMPTY_SUBMISSION → 0 XP, Uziladi"). AI_SKIPPED does <i>not</i>
 * award XP here — TZ §4 is explicit that XP only happens "AI_SKIPPED holatida — faqat o'qituvchi
 * qo'lda baholagach" (only once a teacher manually grades it), handled in
 * TeacherSubmissionService.grade() instead. Grading criteria come from {@link
 * GradingCriteriaService} (Sprint 3, TZ §1.19/§2.3) when the teacher has configured them for the
 * assignment's subject, falling back to the spec's own documented default set (the exact
 * 3-criterion example from TZ §3.2) when unconfigured — TZ §1.19 explicitly allows this fallback.
 */
@Service
public class AIAnalysisService {

  private static final int MIN_MEANINGFUL_TEXT_LENGTH = 5;

  // academix_tz.md §3.2's own worked example — used as the default until per-subject
  // SubjectGradingCriteria (TZ §1.19) is built.
  private static final List<GradingCriterion> DEFAULT_CRITERIA =
      List.of(
          new GradingCriterion("Yechish usuli", 40),
          new GradingCriterion("Javob to'g'riligi", 30),
          new GradingCriterion("Tushunarlilik", 30));

  private final HomeworkSubmissionRepository submissionRepository;
  private final HomeworkAssignmentRepository assignmentRepository;
  private final SubjectRepository subjectRepository;
  private final SchoolClassRepository classRepository;
  private final AIFeedbackRepository aiFeedbackRepository;
  private final FileStorageService fileStorageService;
  private final GoogleVisionClient googleVisionClient;
  private final QwenAIClient qwenAIClient;
  private final AiBudgetService aiBudgetService;
  private final GradingCriteriaService gradingCriteriaService;
  private final XPService xpService;

  public AIAnalysisService(
      HomeworkSubmissionRepository submissionRepository,
      HomeworkAssignmentRepository assignmentRepository,
      SubjectRepository subjectRepository,
      SchoolClassRepository classRepository,
      AIFeedbackRepository aiFeedbackRepository,
      FileStorageService fileStorageService,
      GoogleVisionClient googleVisionClient,
      QwenAIClient qwenAIClient,
      AiBudgetService aiBudgetService,
      GradingCriteriaService gradingCriteriaService,
      XPService xpService) {
    this.submissionRepository = submissionRepository;
    this.assignmentRepository = assignmentRepository;
    this.subjectRepository = subjectRepository;
    this.classRepository = classRepository;
    this.aiFeedbackRepository = aiFeedbackRepository;
    this.fileStorageService = fileStorageService;
    this.googleVisionClient = googleVisionClient;
    this.qwenAIClient = qwenAIClient;
    this.aiBudgetService = aiBudgetService;
    this.gradingCriteriaService = gradingCriteriaService;
    this.xpService = xpService;
  }

  public void analyzeSubmission(UUID submissionId) {
    HomeworkSubmissionEntity subEntity =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Queue message for unknown submissionId " + submissionId));
    HomeworkSubmission submission = subEntity.toDomain();
    updateStatus(submission, SubmissionStatus.AI_PROCESSING);

    String extractedText;
    try {
      extractedText = extractText(submission);
    } catch (OcrUnavailableException e) {
      // OCR is a hard dependency for IMAGE/MIXED — no text, nothing to grade. Same
      // graceful-degradation outcome as a budget-exhausted submission (§8): accepted, not graded.
      saveOcrOnlyFeedback(submission, "");
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }

    if (extractedText == null || extractedText.trim().length() < MIN_MEANINGFUL_TEXT_LENGTH) {
      // Empty/meaningless submission — no AI call, but still breaks the streak (§4 XP table).
      saveEmptyFeedback(submission);
      updateStatus(submission, SubmissionStatus.AI_DONE);
      xpService.calculateAndAwardXP(submission.id(), 0f, submission.isLate());
      xpService.updateStreak(submission.studentId(), 0f);
      xpService.checkAndAwardBadges(submission.studentId());
      return;
    }

    if (!aiBudgetService.isWithinAiBudget(submission.schoolId(), AiCallCategory.HOMEWORK)) {
      saveOcrOnlyFeedback(submission, extractedText);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }

    HomeworkAssignmentEntity assignment =
        assignmentRepository
            .findById(submission.assignmentId())
            .orElseThrow(() -> new IllegalStateException("Assignment missing for submission"));
    String subjectAndGrade = subjectAndGrade(assignment);
    List<GradingCriterion> criteria = resolveCriteria(assignment);

    QwenGradingResult result;
    try {
      result = qwenAIClient.gradeSubmission(subjectAndGrade, criteria, extractedText);
    } catch (QwenUnavailableException e) {
      saveOcrOnlyFeedback(submission, extractedText);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }
    aiBudgetService.recordAiUsage(submission.schoolId(), AiCallCategory.HOMEWORK);

    float aiScorePercent = weightedSum(result.criteriaScores());
    saveGradedFeedback(submission, extractedText, result, aiScorePercent);
    updateStatus(submission, SubmissionStatus.AI_DONE);
    xpService.calculateAndAwardXP(submission.id(), aiScorePercent, submission.isLate());
    xpService.updateStreak(submission.studentId(), aiScorePercent);
    xpService.checkAndAwardBadges(submission.studentId());
  }

  private String extractText(HomeworkSubmission submission) {
    boolean hasImage = submission.imageUrl() != null && !submission.imageUrl().isBlank();
    if (submission.type() == SubmissionType.TEXT || !hasImage) {
      return submission.textContent();
    }
    byte[] imageBytes = fileStorageService.download(submission.imageUrl());
    String ocrText = googleVisionClient.extractText(imageBytes).extractedText();
    if (submission.type() == SubmissionType.MIXED
        && submission.textContent() != null
        && !submission.textContent().isBlank()) {
      return submission.textContent() + "\n" + ocrText;
    }
    return ocrText;
  }

  private List<GradingCriterion> resolveCriteria(HomeworkAssignmentEntity assignment) {
    List<GradingCriterion> configured =
        gradingCriteriaService.getForGrading(assignment.getTeacherId(), assignment.getSubjectId());
    return configured.isEmpty() ? DEFAULT_CRITERIA : configured;
  }

  private String subjectAndGrade(HomeworkAssignmentEntity assignment) {
    String subjectName =
        subjectRepository
            .findById(assignment.getSubjectId())
            .map(SubjectEntity::getName)
            .orElse("Fan");
    Integer grade =
        classRepository
            .findById(assignment.getClassId())
            .map(SchoolClassEntity::getGrade)
            .orElse(null);
    return grade == null ? subjectName : subjectName + " " + grade + "-sinf";
  }

  private static float weightedSum(List<CriteriaScore> criteriaScores) {
    double sum = 0;
    for (CriteriaScore score : criteriaScores) {
      sum += score.score() * score.weightPercent();
    }
    return (float) (sum / 100.0);
  }

  private void saveEmptyFeedback(HomeworkSubmission submission) {
    AIFeedback feedback =
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
            PlagiarismType.CLEAN,
            0f,
            LocalDateTime.now());
    aiFeedbackRepository.save(AIFeedbackEntity.fromDomain(feedback));
  }

  private void saveOcrOnlyFeedback(HomeworkSubmission submission, String extractedText) {
    AIFeedback feedback =
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
            PlagiarismType.CLEAN,
            0f,
            LocalDateTime.now());
    aiFeedbackRepository.save(AIFeedbackEntity.fromDomain(feedback));
  }

  private void saveGradedFeedback(
      HomeworkSubmission submission,
      String extractedText,
      QwenGradingResult result,
      float aiScorePercent) {
    AIFeedback feedback =
        new AIFeedback(
            UUID.randomUUID(),
            submission.id(),
            extractedText,
            0f,
            result.stepAnalyses(),
            result.criteriaScores(),
            aiScorePercent,
            result.feedback(),
            null,
            (float) result.plagiarismScore(),
            parsePlagiarismType(result.plagiarismType()),
            0f,
            LocalDateTime.now());
    aiFeedbackRepository.save(AIFeedbackEntity.fromDomain(feedback));
  }

  private static PlagiarismType parsePlagiarismType(String raw) {
    try {
      return PlagiarismType.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException e) {
      return PlagiarismType.CLEAN;
    }
  }

  private void updateStatus(HomeworkSubmission submission, SubmissionStatus status) {
    HomeworkSubmission updated =
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
            submission.xpEarned());
    submissionRepository.save(HomeworkSubmissionEntity.fromDomain(updated));
  }
}
