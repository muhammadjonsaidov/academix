package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.14 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.PsychologicalSignalEntity}. No {@code schoolId} field — this table
 * isn't RLS-enabled (backend_tdd.md §4.1 table 13 has no school_id column); callers must scope via
 * the joined {@code student_profiles.school_id}, same shape as grades/exam_grades. {@code
 * resolutionNotes}/{@code actionTaken} are a V33 deviation — see that migration's comment.
 */
public record PsychologicalSignal(
    UUID id,
    UUID studentId,
    SignalType type,
    SignalSeverity severity,
    String description,
    String rawEvidence,
    boolean isManipulation,
    boolean notifiedClassTeacher,
    boolean notifiedParent,
    boolean notifiedPsychologist,
    boolean resolved,
    LocalDateTime detectedAt,
    LocalDateTime resolvedAt,
    String resolutionNotes,
    String actionTaken) {}
