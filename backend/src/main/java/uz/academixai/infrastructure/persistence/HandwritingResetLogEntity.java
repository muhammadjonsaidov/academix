package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.HandwritingResetLog;
import uz.academixai.domain.ResetReason;

/**
 * JPA mapping for {@code handwriting_reset_logs} (migration V24). Maps to/from {@link
 * HandwritingResetLog}.
 */
@Entity
@Table(name = "handwriting_reset_logs")
public class HandwritingResetLogEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ResetReason reason;

  private String notes;

  @Column(name = "previous_profile_version")
  private String previousProfileVersion;

  @Column(name = "reset_at")
  private LocalDateTime resetAt;

  protected HandwritingResetLogEntity() {}

  public HandwritingResetLogEntity(
      UUID id,
      UUID schoolId,
      UUID studentId,
      UUID teacherId,
      ResetReason reason,
      String notes,
      String previousProfileVersion,
      LocalDateTime resetAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.studentId = studentId;
    this.teacherId = teacherId;
    this.reason = reason;
    this.notes = notes;
    this.previousProfileVersion = previousProfileVersion;
    this.resetAt = resetAt;
  }

  public static HandwritingResetLogEntity fromDomain(HandwritingResetLog domain) {
    return new HandwritingResetLogEntity(
        domain.id(),
        domain.schoolId(),
        domain.studentId(),
        domain.teacherId(),
        domain.reason(),
        domain.notes(),
        domain.previousProfileVersion(),
        domain.resetAt());
  }

  public HandwritingResetLog toDomain() {
    return new HandwritingResetLog(
        id, schoolId, studentId, teacherId, reason, notes, previousProfileVersion, resetAt);
  }
}
