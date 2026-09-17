package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExamRequest(
    @NotNull UUID classId,
    @NotNull UUID subjectId,
    @NotBlank @Size(max = 255) String title,
    @NotNull LocalDate examDate,
    @Min(1) @Max(1000) int maxScore) {}
