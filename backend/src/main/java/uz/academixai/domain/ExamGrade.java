package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Deviation, judgment call: not documented in academix_tz.md/backend_tdd.md — {@code grades.
 * submission_id} FK is scoped to {@code homework_submissions} only, so exam grading needs its own
 * table. Mirrors {@link Grade} exactly (same fields, same "no school_id/no RLS" shape — access is
 * controlled via the RLS-enabled {@code exam_submissions} join, same as {@code grades} relies on
 * {@code homework_submissions}), just FK'd to {@code exam_submissions} instead.
 */
public record ExamGrade(
    UUID id,
    UUID examSubmissionId,
    UUID teacherId,
    int score,
    int fivePointGrade,
    String teacherComment,
    LocalDateTime gradedAt) {}
