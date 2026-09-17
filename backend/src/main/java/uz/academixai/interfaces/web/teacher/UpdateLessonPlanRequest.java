package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLessonPlanRequest(
    @NotBlank @Size(max = 20000) String teacherEditedPlan, boolean isApproved) {}
