package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLessonPlanRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 20000, message = "ko'pi bilan 20000 belgi")
        String teacherEditedPlan,
    boolean isApproved) {}
