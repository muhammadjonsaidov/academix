package uz.academixai.interfaces.web.teacher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.LessonPlan;

public record LessonPlanResponse(
    UUID lessonPlanId,
    UUID subjectId,
    UUID classId,
    UUID syllabusId,
    String topic,
    LessonPlanContentDto aiGeneratedPlan,
    String teacherEditedPlan,
    boolean isApproved,
    LocalDate lessonDate,
    LocalDateTime createdAt) {

  public static LessonPlanResponse from(LessonPlan plan) {
    return new LessonPlanResponse(
        plan.id(),
        plan.subjectId(),
        plan.classId(),
        plan.syllabusId(),
        plan.topic(),
        LessonPlanContentDto.from(plan.aiGeneratedPlan()),
        plan.teacherEditedPlan(),
        plan.isApproved(),
        plan.lessonDate(),
        plan.createdAt());
  }
}
