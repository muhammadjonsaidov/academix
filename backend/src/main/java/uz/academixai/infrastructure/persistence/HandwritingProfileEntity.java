package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.HandwritingProfile;
import uz.academixai.domain.ResetReason;

/**
 * JPA mapping for {@code handwriting_profiles} (migration V23), <b>excluding</b> {@code
 * feature_vector} — pgvector's {@code vector} column type has no Hibernate mapping in this
 * project's stack, so it's read/written via native queries in {@code HandwritingService} instead.
 * Deliberately safe: this entity's generated INSERT/UPDATE statements never reference that column,
 * so a plain {@code repository.save(...)} here can never accidentally null it out.
 */
@Entity
@Table(name = "handwriting_profiles")
public class HandwritingProfileEntity {

  @Id private UUID id;

  @Column(name = "student_id", nullable = false, unique = true)
  private UUID studentId;

  @Column(name = "samples_count")
  private int samplesCount;

  @Column(name = "is_reliable")
  private boolean isReliable;

  @Column(name = "profile_version")
  private String profileVersion;

  @Column(name = "last_updated_at")
  private LocalDateTime lastUpdatedAt;

  @Column(name = "last_reset_by_teacher_id")
  private UUID lastResetByTeacherId;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_reset_reason")
  private ResetReason lastResetReason;

  @Column(name = "last_reset_at")
  private LocalDateTime lastResetAt;

  @Column(name = "reset_count_this_semester")
  private int resetCountThisSemester;

  protected HandwritingProfileEntity() {}

  public HandwritingProfileEntity(
      UUID id,
      UUID studentId,
      int samplesCount,
      boolean isReliable,
      String profileVersion,
      LocalDateTime lastUpdatedAt,
      UUID lastResetByTeacherId,
      ResetReason lastResetReason,
      LocalDateTime lastResetAt,
      int resetCountThisSemester) {
    this.id = id;
    this.studentId = studentId;
    this.samplesCount = samplesCount;
    this.isReliable = isReliable;
    this.profileVersion = profileVersion;
    this.lastUpdatedAt = lastUpdatedAt;
    this.lastResetByTeacherId = lastResetByTeacherId;
    this.lastResetReason = lastResetReason;
    this.lastResetAt = lastResetAt;
    this.resetCountThisSemester = resetCountThisSemester;
  }

  /** {@code featureVector} is intentionally dropped — see class doc. */
  public static HandwritingProfileEntity fromDomain(HandwritingProfile domain) {
    return new HandwritingProfileEntity(
        domain.id(),
        domain.studentId(),
        domain.samplesCount(),
        domain.isReliable(),
        domain.profileVersion(),
        domain.lastUpdatedAt(),
        domain.lastResetByTeacherId(),
        domain.lastResetReason(),
        domain.lastResetAt(),
        domain.resetCountThisSemester());
  }

  /** {@code featureVector} is always null here — callers needing it must read it separately. */
  public HandwritingProfile toDomain() {
    return new HandwritingProfile(
        id,
        studentId,
        samplesCount,
        isReliable,
        null,
        profileVersion,
        lastUpdatedAt,
        lastResetByTeacherId,
        lastResetReason,
        lastResetAt,
        resetCountThisSemester);
  }

  public UUID getId() {
    return id;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public int getSamplesCount() {
    return samplesCount;
  }

  public boolean isReliable() {
    return isReliable;
  }

  public String getProfileVersion() {
    return profileVersion;
  }

  public int getResetCountThisSemester() {
    return resetCountThisSemester;
  }
}
