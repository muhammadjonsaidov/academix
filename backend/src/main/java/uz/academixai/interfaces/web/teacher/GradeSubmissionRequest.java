package uz.academixai.interfaces.web.teacher;

/** academix_tz.md §2.3 — POST /teacher/submissions/{submissionId}/grade */
public record GradeSubmissionRequest(int score, int fivePointGrade, String teacherComment) {}
