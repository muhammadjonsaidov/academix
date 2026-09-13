package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.academixai.domain.PsychologicalSignal;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.domain.SignalType;

/**
 * JPA mapping for {@code psychological_signals} (backend_tdd.md §4.1 table 13). Maps to/from {@link
 * PsychologicalSignal}.
 *
 * <p>{@code rawEvidence} is a {@code String} mapped {@code @JdbcTypeCode(SqlTypes.JSON)} against a
 * {@code jsonb} column — per the lesson in CLAUDE.md's {@code lesson_plans.teacher_edited_plan}
 * incident, this means the string's CONTENTS must themselves already be a valid JSON document (e.g.
 * a JSON-encoded string literal), not arbitrary free text — the Wellbeing persistence adapter must
 * receive evidence serialized by its dedicated port, never a raw provider evidence string.
 */
@Entity
@Table(name = "psychological_signals")
public class PsychologicalSignalEntity {

  @Id private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SignalType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SignalSeverity severity;

  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_evidence")
  private String rawEvidence;

  @Column(name = "is_manipulation")
  private boolean isManipulation;

  @Column(name = "notified_class_teacher")
  private boolean notifiedClassTeacher;

  @Column(name = "notified_parent")
  private boolean notifiedParent;

  @Column(name = "notified_psychologist")
  private boolean notifiedPsychologist;

  private boolean resolved;

  @Column(name = "detected_at")
  private LocalDateTime detectedAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "resolution_notes")
  private String resolutionNotes;

  @Column(name = "action_taken")
  private String actionTaken;

  protected PsychologicalSignalEntity() {}

  public PsychologicalSignalEntity(
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
      String actionTaken) {
    this.id = id;
    this.studentId = studentId;
    this.type = type;
    this.severity = severity;
    this.description = description;
    this.rawEvidence = rawEvidence;
    this.isManipulation = isManipulation;
    this.notifiedClassTeacher = notifiedClassTeacher;
    this.notifiedParent = notifiedParent;
    this.notifiedPsychologist = notifiedPsychologist;
    this.resolved = resolved;
    this.detectedAt = detectedAt;
    this.resolvedAt = resolvedAt;
    this.resolutionNotes = resolutionNotes;
    this.actionTaken = actionTaken;
  }

  public static PsychologicalSignalEntity fromDomain(PsychologicalSignal domain) {
    return new PsychologicalSignalEntity(
        domain.id(),
        domain.studentId(),
        domain.type(),
        domain.severity(),
        domain.description(),
        domain.rawEvidence(),
        domain.isManipulation(),
        domain.notifiedClassTeacher(),
        domain.notifiedParent(),
        domain.notifiedPsychologist(),
        domain.resolved(),
        domain.detectedAt(),
        domain.resolvedAt(),
        domain.resolutionNotes(),
        domain.actionTaken());
  }

  public PsychologicalSignal toDomain() {
    return new PsychologicalSignal(
        id,
        studentId,
        type,
        severity,
        description,
        rawEvidence,
        isManipulation,
        notifiedClassTeacher,
        notifiedParent,
        notifiedPsychologist,
        resolved,
        detectedAt,
        resolvedAt,
        resolutionNotes,
        actionTaken);
  }

  public UUID getId() {
    return id;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public SignalSeverity getSeverity() {
    return severity;
  }

  public boolean isResolved() {
    return resolved;
  }
}
