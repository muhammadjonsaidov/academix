package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GradeExamSubmissionRequest(
    @Min(value = 0, message = "kamida 0") int score,
    @Min(value = 1, message = "kamida 1") @Max(value = 5, message = "ko'pi bilan 5")
        int fivePointGrade,
    @Size(max = 2000, message = "ko'pi bilan 2000 belgi") String teacherComment) {}
