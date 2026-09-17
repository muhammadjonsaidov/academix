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
    @NotNull(message = "majburiy maydon") UUID classId,
    @NotNull(message = "majburiy maydon") UUID subjectId,
    @NotBlank(message = "majburiy maydon") @Size(max = 255, message = "ko'pi bilan 255 belgi")
        String title,
    @Size(max = 5000, message = "ko'pi bilan 5000 belgi") String description,
    @NotNull(message = "majburiy maydon") LocalDateTime deadlineAt,
    AssignmentType type,
    @Size(max = 255, message = "ko'pi bilan 255 belgi") String syllabusReference,
    @Min(value = 1, message = "kamida 1") @Max(value = 1000, message = "ko'pi bilan 1000")
        int maxScore) {}
