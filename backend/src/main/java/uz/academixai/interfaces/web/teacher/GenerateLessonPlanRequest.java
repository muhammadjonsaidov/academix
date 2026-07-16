package uz.academixai.interfaces.web.teacher;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateLessonPlanRequest(
    UUID syllabusId, String topic, LocalDate lessonDate, UUID classId) {}
