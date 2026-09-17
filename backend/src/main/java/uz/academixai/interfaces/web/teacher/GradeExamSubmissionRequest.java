package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GradeExamSubmissionRequest(
    @Min(0) int score,
    @Min(1) @Max(5) int fivePointGrade,
    @Size(max = 2000) String teacherComment) {}
