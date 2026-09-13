package uz.academixai.application;

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
import uz.academixai.infrastructure.ai.AiProviderUnavailableException;
import uz.academixai.infrastructure.ai.OpenAiCompatibleClient;
import uz.academixai.infrastructure.persistence.LessonPlanEntity;
import uz.academixai.infrastructure.persistence.LessonPlanRepository;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.SubjectEntity;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.persistence.TeacherSyllabusEntity;
import uz.academixai.infrastructure.persistence.TeacherSyllabusRepository;
import uz.academixai.interfaces.web.ApiException;

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
 */
@Service
public class LessonPlanService {

  private static final Logger log = LoggerFactory.getLogger(LessonPlanService.class);

  private final LessonPlanRepository lessonPlanRepository;
  private final TeacherSyllabusRepository syllabusRepository;
  private final SubjectRepository subjectRepository;
  private final SchoolClassRepository classRepository;
  private final OpenAiCompatibleClient aiClient;

  public LessonPlanService(
      LessonPlanRepository lessonPlanRepository,
      TeacherSyllabusRepository syllabusRepository,
      SubjectRepository subjectRepository,
      SchoolClassRepository classRepository,
      OpenAiCompatibleClient aiClient) {
    this.lessonPlanRepository = lessonPlanRepository;
    this.syllabusRepository = syllabusRepository;
    this.subjectRepository = subjectRepository;
    this.classRepository = classRepository;
    this.aiClient = aiClient;
  }

  public LessonPlan generate(
      UUID teacherId, UUID syllabusId, String topic, LocalDate lessonDate, UUID classId) {
    TeacherSyllabusEntity syllabus =
        syllabusRepository
            .findByIdAndTeacherId(syllabusId, teacherId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_SYLLABUS_NOT_FOUND",
                        "Darslik topilmadi.",
                        "syllabusId ni tekshiring."));
    UUID subjectId = syllabus.toDomain().subjectId();
    String subjectAndGrade = subjectAndGrade(subjectId, classId);

    var content = callGenerate(subjectAndGrade, topic, syllabus.toDomain().extractedContent());

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
    return lessonPlanRepository.save(LessonPlanEntity.fromDomain(plan)).toDomain();
  }

  public List<LessonPlan> list(UUID teacherId, UUID subjectId, UUID classId) {
    var entities =
        switch ((subjectId != null ? 1 : 0) + (classId != null ? 2 : 0)) {
          case 3 ->
              lessonPlanRepository.findByTeacherIdAndSubjectIdAndClassIdOrderByLessonDateDesc(
                  teacherId, subjectId, classId);
          case 2 ->
              lessonPlanRepository.findByTeacherIdAndClassIdOrderByLessonDateDesc(
                  teacherId, classId);
          case 1 ->
              lessonPlanRepository.findByTeacherIdAndSubjectIdOrderByLessonDateDesc(
                  teacherId, subjectId);
          default -> lessonPlanRepository.findByTeacherIdOrderByLessonDateDesc(teacherId);
        };
    return entities.stream().map(LessonPlanEntity::toDomain).toList();
  }

  public LessonPlan update(
      UUID teacherId, UUID planId, String teacherEditedPlan, boolean isApproved) {
    LessonPlanEntity entity =
        lessonPlanRepository
            .findByIdAndTeacherId(planId, teacherId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_LESSON_PLAN_NOT_FOUND",
                        "Dars rejasi topilmadi.",
                        "ID ni tekshiring."));
    LessonPlan existing = entity.toDomain();
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
    return lessonPlanRepository.save(LessonPlanEntity.fromDomain(updated)).toDomain();
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
      log.warn("Qwen lesson-plan generation unavailable", e);
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "ERR_AI_UNAVAILABLE",
          "AI hozircha ishlamayapti — birozdan so'ng qayta urinib ko'ring.",
          "Birozdan so'ng qayta urinib ko'ring.");
    }
  }

  private String subjectAndGrade(UUID subjectId, UUID classId) {
    String subjectName =
        subjectRepository.findById(subjectId).map(SubjectEntity::getName).orElse("Fan");
    Integer grade = classRepository.findById(classId).map(SchoolClassEntity::getGrade).orElse(null);
    return grade == null ? subjectName : subjectName + " " + grade + "-sinf";
  }
}
