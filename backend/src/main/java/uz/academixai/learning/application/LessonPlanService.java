package uz.academixai.learning.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.LessonPlan;
import uz.academixai.domain.LessonPlanContent;
import uz.academixai.learning.application.port.out.ClassGradeLookup;
import uz.academixai.learning.application.port.out.LessonPlanStore;
import uz.academixai.learning.application.port.out.SubjectNameLookup;
import uz.academixai.learning.application.port.out.SyllabusStore;
import uz.academixai.learning.domain.SyllabusProcessingStatus;
import uz.academixai.learning.domain.TeacherSyllabus;
import uz.academixai.shared.ai.AiProvider;
import uz.academixai.shared.ai.AiProviderUnavailableException;
import uz.academixai.shared.error.ApiException;

/**
 * academix_tz.md §1.18/§2.3 "Dars rejasi". No AI budget gate — lesson-plan generation isn't one of
 * the 3 sub-budget categories in TZ §8 (EXAM/HOMEWORK/CHAT only), matching that exact enum rather
 * than inventing a 4th category.
 *
 * <p><b>Deviation, judgment call (see ROADMAP.md Sprint 3):</b> TZ §2.3's generate request body is
 * {@code { syllabusId, topic, lessonDate, classId } } — no {@code subjectId}, but {@code
 * LessonPlan.subjectId} is NOT NULL. {@code subjectId} is derived from the referenced syllabus,
 * which makes {@code syllabusId} effectively required here even though the spec doesn't say so
 * explicitly.
 *
 * <p>Moved here from the legacy {@code application} package. It reached four repositories directly;
 * three of those facts already had Learning ports ({@link SyllabusStore}, {@link
 * SubjectNameLookup}) and only the class grade needed a new one, so the move deleted three
 * dependencies rather than adding four.
 */
@Service
public class LessonPlanService {

  private static final Logger log = LoggerFactory.getLogger(LessonPlanService.class);

  private final LessonPlanStore lessonPlans;
  private final SyllabusStore syllabuses;
  private final SubjectNameLookup subjectNames;
  private final ClassGradeLookup classGrades;
  private final AiProvider aiClient;
  private final SyllabusKnowledgeService syllabusKnowledge;

  public LessonPlanService(
      LessonPlanStore lessonPlans,
      SyllabusStore syllabuses,
      SubjectNameLookup subjectNames,
      ClassGradeLookup classGrades,
      AiProvider aiClient,
      SyllabusKnowledgeService syllabusKnowledge) {
    this.lessonPlans = lessonPlans;
    this.syllabuses = syllabuses;
    this.subjectNames = subjectNames;
    this.classGrades = classGrades;
    this.aiClient = aiClient;
    this.syllabusKnowledge = syllabusKnowledge;
  }

  public LessonPlan generate(
      UUID teacherId, UUID syllabusId, String topic, LocalDate lessonDate, UUID classId) {
    TeacherSyllabus syllabus =
        syllabuses
            .findOwned(teacherId, syllabusId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_SYLLABUS_NOT_FOUND",
                        "Darslik topilmadi.",
                        "syllabusId ni tekshiring."));
    UUID subjectId = syllabus.subjectId();
    if (!syllabus.classId().equals(classId)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "ERR_SYLLABUS_CLASS_MISMATCH",
          "Darslik tanlangan sinfga tegishli emas.",
          "Darslik va sinfni bir xil qilib tanlang.");
    }
    if (syllabus.processingStatus() != SyllabusProcessingStatus.READY) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_SYLLABUS_NOT_READY",
          syllabus.processingStatus() == SyllabusProcessingStatus.FAILED
              ? "Darslik AI uchun tayyorlanmadi. Uni qayta yuklang."
              : "Darslik hali AI uchun tayyorlanmoqda.",
          "Darslik tayyor bo'lgach qayta urinib ko'ring.");
    }
    String subjectAndGrade = subjectAndGrade(subjectId, classId);
    String groundedContext =
        String.join(
            "\n\n---\n\n", syllabusKnowledge.relevantForSyllabus(teacherId, syllabusId, topic, 6));
    if (groundedContext.isBlank()) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "ERR_SYLLABUS_NOT_READY",
          "Darslikdan AI uchun kontekst topilmadi.",
          "Darslikni qayta yuklang.");
    }
    var content = callGenerate(subjectAndGrade, topic, groundedContext);

    LessonPlan plan =
        new LessonPlan(
            UUID.randomUUID(),
            teacherId,
            subjectId,
            classId,
            syllabusId,
            topic,
            content,
            null,
            false,
            lessonDate,
            LocalDateTime.now());
    return lessonPlans.save(plan);
  }

  public List<LessonPlan> list(UUID teacherId, UUID subjectId, UUID classId) {
    return lessonPlans.list(teacherId, subjectId, classId);
  }

  public LessonPlan update(
      UUID teacherId, UUID planId, String teacherEditedPlan, boolean isApproved) {
    LessonPlan existing =
        lessonPlans
            .findOwned(teacherId, planId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_LESSON_PLAN_NOT_FOUND",
                        "Dars rejasi topilmadi.",
                        "ID ni tekshiring."));
    LessonPlan updated =
        new LessonPlan(
            existing.id(),
            existing.teacherId(),
            existing.subjectId(),
            existing.classId(),
            existing.syllabusId(),
            existing.topic(),
            existing.aiGeneratedPlan(),
            teacherEditedPlan,
            isApproved,
            existing.lessonDate(),
            existing.createdAt());
    return lessonPlans.save(updated);
  }

  private LessonPlanContent callGenerate(
      String subjectAndGrade, String topic, String syllabusExtractedContent) {
    try {
      return aiClient.generateLessonPlan(subjectAndGrade, topic, syllabusExtractedContent);
    } catch (AiProviderUnavailableException e) {
      // ApiException bypasses GlobalExceptionHandler's logging (only its catch-all
      // Exception.class handler logs) — without this, the real cause (auth failure,
      // timeout, malformed JSON, genuine circuit-open) was silently swallowed, confirmed
      // by a real 503 with zero corresponding log line.
      log.warn("AI provider lesson-plan generation unavailable", e);
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "ERR_AI_UNAVAILABLE",
          "AI hozircha ishlamayapti — birozdan so'ng qayta urinib ko'ring.",
          "Birozdan so'ng qayta urinib ko'ring.");
    }
  }

  private String subjectAndGrade(UUID subjectId, UUID classId) {
    String subjectName = subjectNames.name(subjectId);
    Integer grade = classGrades.gradeOf(classId).orElse(null);
    return grade == null ? subjectName : subjectName + " " + grade + "-sinf";
  }
}
