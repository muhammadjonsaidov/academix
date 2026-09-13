package uz.academixai.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.academixai.application.port.out.ai.AiGradingResult;
import uz.academixai.application.port.out.ai.AiProvider;
import uz.academixai.application.port.out.ai.AiProviderUnavailableException;
import uz.academixai.application.port.out.ai.GradingCriterion;
import uz.academixai.domain.AIFeedback;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.HandwritingCheckResult;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.PlagiarismType;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;
import uz.academixai.infrastructure.ai.AiBudgetService;
import uz.academixai.infrastructure.ai.AiCallCategory;
import uz.academixai.infrastructure.ai.DocumentTextLayout;
import uz.academixai.infrastructure.ai.GoogleVisionClient;
import uz.academixai.infrastructure.ai.OcrUnavailableException;
import uz.academixai.infrastructure.persistence.AIFeedbackEntity;
import uz.academixai.infrastructure.persistence.AIFeedbackRepository;
import uz.academixai.infrastructure.persistence.AiUsageLogEntity;
import uz.academixai.infrastructure.persistence.AiUsageLogRepository;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentEntity;
import uz.academixai.infrastructure.persistence.HomeworkAssignmentRepository;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionEntity;
import uz.academixai.infrastructure.persistence.HomeworkSubmissionRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.realtime.RealtimeEventBus;
import uz.academixai.infrastructure.storage.FileStorageService;
import uz.academixai.progress.application.XPService;

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

  private static final Logger log = LoggerFactory.getLogger(AIAnalysisService.class);

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
  private final AiUsageLogRepository aiUsageLogRepository;
  private final FileStorageService fileStorageService;
  private final GoogleVisionClient googleVisionClient;
  private final AiProvider aiClient;
  private final AiBudgetService aiBudgetService;
  private final GradingCriteriaService gradingCriteriaService;
  private final XPService xpService;
  private final HandwritingService handwritingService;
  private final RealtimeEventBus realtimeEventBus;

  public AIAnalysisService(
      HomeworkSubmissionRepository submissionRepository,
      HomeworkAssignmentRepository assignmentRepository,
      SubjectRepository subjectRepository,
      SchoolClassRepository classRepository,
      AIFeedbackRepository aiFeedbackRepository,
      AiUsageLogRepository aiUsageLogRepository,
      FileStorageService fileStorageService,
      GoogleVisionClient googleVisionClient,
      AiProvider aiClient,
      AiBudgetService aiBudgetService,
      GradingCriteriaService gradingCriteriaService,
      XPService xpService,
      HandwritingService handwritingService,
      RealtimeEventBus realtimeEventBus) {
    this.submissionRepository = submissionRepository;
    this.assignmentRepository = assignmentRepository;
    this.subjectRepository = subjectRepository;
    this.classRepository = classRepository;
    this.aiFeedbackRepository = aiFeedbackRepository;
    this.aiUsageLogRepository = aiUsageLogRepository;
    this.fileStorageService = fileStorageService;
    this.googleVisionClient = googleVisionClient;
    this.aiClient = aiClient;
    this.aiBudgetService = aiBudgetService;
    this.gradingCriteriaService = gradingCriteriaService;
    this.xpService = xpService;
    this.handwritingService = handwritingService;
    this.realtimeEventBus = realtimeEventBus;
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

    ExtractedContent content;
    try {
      content = extractText(submission);
    } catch (OcrUnavailableException e) {
      // OCR is a hard dependency for IMAGE/MIXED — no text, nothing to grade. Same
      // graceful-degradation outcome as a budget-exhausted submission (§8): accepted, not graded.
      saveOcrOnlyFeedback(submission, "", null);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }
    String extractedText = content.text();

    // Handwriting check doesn't touch the Qwen/AI budget (no external call, no cost — TZ §8's 3
    // sub-budgets are EXAM/HOMEWORK/CHAT only) and only applies when Vision actually ran (IMAGE/
    // MIXED submissions), so it runs unconditionally here, before any of the branches below.
    HandwritingCheckResult handwritingResult =
        content.layout() == null
            ? null
            : handwritingService.checkAndUpdateProfile(submission.studentId(), content.layout());

    if (extractedText == null || extractedText.trim().length() < MIN_MEANINGFUL_TEXT_LENGTH) {
      // Empty/meaningless submission — no AI call, but still breaks the streak (§4 XP table).
      saveEmptyFeedback(submission, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_DONE);
      xpService.calculateAndAwardXP(submission.id(), 0f, submission.isLate());
      xpService.updateStreak(submission.studentId(), 0f);
      xpService.checkAndAwardBadges(submission.studentId());
      return;
    }

    if (!aiBudgetService.isWithinAiBudget(submission.schoolId(), AiCallCategory.HOMEWORK)) {
      saveOcrOnlyFeedback(submission, extractedText, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }

    HomeworkAssignmentEntity assignment =
        assignmentRepository
            .findById(submission.assignmentId())
            .orElseThrow(() -> new IllegalStateException("Assignment missing for submission"));
    String subjectAndGrade = subjectAndGrade(assignment);
    List<GradingCriterion> criteria = resolveCriteria(assignment);

    AiGradingResult result;
    try {
      result = aiClient.gradeSubmission(subjectAndGrade, criteria, extractedText);
    } catch (AiProviderUnavailableException e) {
      // Correct degradation (AI_SKIPPED, per TZ §8), but silent — undiagnosable without a log
      // line why grading actually failed (auth, timeout, malformed JSON, circuit open).
      log.warn(
          "AI provider grading unavailable for submission {}, falling to AI_SKIPPED",
          submission.id(),
          e);
      saveOcrOnlyFeedback(submission, extractedText, handwritingResult);
      updateStatus(submission, SubmissionStatus.AI_SKIPPED);
      return;
    }
    aiBudgetService.recordAiUsage(submission.schoolId(), AiCallCategory.HOMEWORK);
    aiUsageLogRepository.save(
        new AiUsageLogEntity(
            UUID.randomUUID(),
            submission.schoolId(),
            assignment.getClassId(),
            assignment.getSubjectId(),
            assignment.getTeacherId(),
            "HOMEWORK",
            LocalDateTime.now()));

    float aiScorePercent = weightedSum(result.criteriaScores());
    saveGradedFeedback(submission, extractedText, result, aiScorePercent, handwritingResult);
    updateStatus(submission, SubmissionStatus.AI_DONE);
    xpService.calculateAndAwardXP(submission.id(), aiScorePercent, submission.isLate());
    xpService.updateStreak(submission.studentId(), aiScorePercent);
    xpService.checkAndAwardBadges(submission.studentId());
  }

  /** {@code layout} is null when no Vision call happened (TEXT submissions have no image). */
  private record ExtractedContent(String text, DocumentTextLayout layout) {}

  private ExtractedContent extractText(HomeworkSubmission submission) {
    boolean hasImage = submission.imageUrl() != null && !submission.imageUrl().isBlank();
    if (submission.type() == SubmissionType.TEXT || !hasImage) {
      return new ExtractedContent(submission.textContent(), null);
    }
    byte[] imageBytes = fileStorageService.download(submission.imageUrl());
    var ocrResult = googleVisionClient.extractText(imageBytes);
    String ocrText = ocrResult.extractedText();
    if (submission.type() == SubmissionType.MIXED
        && submission.textContent() != null
        && !submission.textContent().isBlank()) {
      return new ExtractedContent(submission.textContent() + "\n" + ocrText, ocrResult.layout());
    }
    return new ExtractedContent(ocrText, ocrResult.layout());
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

  private void saveEmptyFeedback(
      HomeworkSubmission submission, HandwritingCheckResult handwriting) {
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
            resolvePlagiarismType(PlagiarismType.CLEAN, handwriting),
            handwritingScore(handwriting),
            LocalDateTime.now());
    aiFeedbackRepository.save(AIFeedbackEntity.fromDomain(feedback));
  }

  private void saveOcrOnlyFeedback(
      HomeworkSubmission submission, String extractedText, HandwritingCheckResult handwriting) {
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
            resolvePlagiarismType(PlagiarismType.CLEAN, handwriting),
            handwritingScore(handwriting),
            LocalDateTime.now());
    aiFeedbackRepository.save(AIFeedbackEntity.fromDomain(feedback));
  }

  private void saveGradedFeedback(
      HomeworkSubmission submission,
      String extractedText,
      AiGradingResult result,
      float aiScorePercent,
      HandwritingCheckResult handwriting) {
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
            resolvePlagiarismType(parsePlagiarismType(result.plagiarismType()), handwriting),
            handwritingScore(handwriting),
            LocalDateTime.now());
    aiFeedbackRepository.save(AIFeedbackEntity.fromDomain(feedback));
  }

  private static float handwritingScore(HandwritingCheckResult handwriting) {
    return handwriting == null ? 0f : handwriting.matchScore();
  }

  /**
   * A handwriting mismatch is a stronger, more specific signal than Qwen's own text-based
   * plagiarism read ("this isn't even the same person's handwriting" vs. "this text looks
   * AI-written") — judgment call (see ROADMAP.md Sprint 5): it overrides Qwen's plagiarismType
   * rather than the two being combined some other way, since {@code ai_feedbacks} has only one
   * plagiarism_type column to store either signal in.
   */
  private static PlagiarismType resolvePlagiarismType(
      PlagiarismType qwenType, HandwritingCheckResult handwriting) {
    return handwriting != null && handwriting.type() == PlagiarismType.HANDWRITING_MISMATCH
        ? PlagiarismType.HANDWRITING_MISMATCH
        : qwenType;
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
    // Live push: the submission's teacher and student get every status transition
    // (AI_PROCESSING → AI_DONE / AI_SKIPPED) so the UI can refresh without polling.
    assignmentRepository
        .findById(submission.assignmentId())
        .ifPresent(
            assignment -> {
              Map<String, Object> payload = new java.util.HashMap<>();
              payload.put("submissionId", submission.id().toString());
              payload.put("type", "HOMEWORK");
              payload.put("status", status.name());
              realtimeEventBus.publishAfterCommit(assignment.getTeacherId(), "ai.status", payload);
              realtimeEventBus.publishAfterCommit(submission.studentId(), "ai.status", payload);
            });
  }
}
