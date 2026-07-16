package uz.academixai.interfaces.web.teacher;

/**
 * academix_tz.md §2.3 — POST /teacher/submissions/{submissionId}/grade. {@code isExcellent} isn't
 * in the spec's request body example — new field, judgment call (see ROADMAP.md Sprint 4) to let a
 * teacher trigger TZ §4's "A'lo" +20 XP bonus. Missing in a request just deserializes to {@code
 * false} (Jackson's default for a primitive boolean), so this is backward compatible.
 */
public record GradeSubmissionRequest(
    int score, int fivePointGrade, String teacherComment, boolean isExcellent) {}
