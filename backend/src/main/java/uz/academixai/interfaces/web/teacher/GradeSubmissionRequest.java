package uz.academixai.interfaces.web.teacher;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * academix_tz.md §2.3 — POST /teacher/submissions/{submissionId}/grade. {@code isExcellent} isn't
 * in the spec's request body example — new field, judgment call (see ROADMAP.md Sprint 4) to let a
 * teacher trigger TZ §4's "A'lo" +20 XP bonus. Missing in a request just deserializes to {@code
 * false} (Jackson's default for a primitive boolean), so this is backward compatible.
 */
public record GradeSubmissionRequest(
    @Min(0) int score,
    @Min(1) @Max(5) int fivePointGrade,
    @Size(max = 2000) String teacherComment,
    boolean isExcellent) {}
