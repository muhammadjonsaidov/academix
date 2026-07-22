package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.PasswordResetLog;

/**
 * JPA mapping for {@code password_reset_logs} (migration V39). Maps to/from {@link
 * PasswordResetLog}.
 */
@Entity
@Table(name = "password_reset_logs")
public class PasswordResetLogEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "reset_at")
  private LocalDateTime resetAt;

  protected PasswordResetLogEntity() {}

  public PasswordResetLogEntity(
      UUID id, UUID schoolId, UUID studentId, UUID teacherId, LocalDateTime resetAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.studentId = studentId;
    this.teacherId = teacherId;
    this.resetAt = resetAt;
  }

  public static PasswordResetLogEntity fromDomain(PasswordResetLog domain) {
    return new PasswordResetLogEntity(
        domain.id(), domain.schoolId(), domain.studentId(), domain.teacherId(), domain.resetAt());
  }

  public PasswordResetLog toDomain() {
    return new PasswordResetLog(id, schoolId, studentId, teacherId, resetAt);
  }
}
