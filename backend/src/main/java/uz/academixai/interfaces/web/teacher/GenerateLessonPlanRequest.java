package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record GenerateLessonPlanRequest(
    @NotNull UUID syllabusId,
    @NotBlank @Size(max = 255) String topic,
    @NotNull LocalDate lessonDate,
    @NotNull UUID classId) {}
