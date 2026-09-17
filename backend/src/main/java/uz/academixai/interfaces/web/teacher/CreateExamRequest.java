package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExamRequest(
    @NotNull(message = "majburiy maydon") UUID classId,
    @NotNull(message = "majburiy maydon") UUID subjectId,
    @NotBlank(message = "majburiy maydon") @Size(max = 255, message = "ko'pi bilan 255 belgi")
        String title,
    @NotNull(message = "majburiy maydon") LocalDate examDate,
    @Min(value = 1, message = "kamida 1") @Max(value = 1000, message = "ko'pi bilan 1000")
        int maxScore) {}
