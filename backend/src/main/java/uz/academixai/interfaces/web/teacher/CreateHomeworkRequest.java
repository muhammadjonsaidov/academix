package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.AssignmentType;

/**
 * academix_tz.md §2.3 — { classId, subjectId, title, description, deadlineAt, type,
 * syllabusReference?, maxScore }
 */
public record CreateHomeworkRequest(
    @NotNull UUID classId,
    @NotNull UUID subjectId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 5000) String description,
    @NotNull LocalDateTime deadlineAt,
    AssignmentType type,
    @Size(max = 255) String syllabusReference,
    @Min(1) @Max(1000) int maxScore) {}
