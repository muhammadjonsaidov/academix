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
    @Min(value = 0, message = "kamida 0") int score,
    @Min(value = 1, message = "kamida 1") @Max(value = 5, message = "ko'pi bilan 5")
        int fivePointGrade,
    @Size(max = 2000, message = "ko'pi bilan 2000 belgi") String teacherComment,
    boolean isExcellent) {}
