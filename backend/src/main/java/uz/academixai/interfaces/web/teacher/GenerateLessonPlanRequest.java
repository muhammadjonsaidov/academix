package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record GenerateLessonPlanRequest(
    @NotNull(message = "majburiy maydon") UUID syllabusId,
    @NotBlank(message = "majburiy maydon") @Size(max = 255, message = "ko'pi bilan 255 belgi")
        String topic,
    @NotNull(message = "majburiy maydon") LocalDate lessonDate,
    @NotNull(message = "majburiy maydon") UUID classId) {}
