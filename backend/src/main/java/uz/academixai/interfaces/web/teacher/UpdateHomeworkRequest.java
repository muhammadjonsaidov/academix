package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** academix_tz.md §2.3 — PUT /teacher/homework/{assignmentId} body. */
public record UpdateHomeworkRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 5000) String description,
    @NotNull LocalDateTime deadlineAt,
    @Size(max = 255) String syllabusReference,
    @Min(1) @Max(1000) int maxScore) {}
