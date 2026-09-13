package uz.academixai.progress.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.progress.domain.StudentBadge;

/** JPA mapping for {@code student_badges} (migration V21). Maps to/from {@link StudentBadge}. */
@Entity
@Table(name = "student_badges")
public class StudentBadgeEntity {

  @Id private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "badge_id", nullable = false)
  private UUID badgeId;

  @Column(name = "awarded_at", nullable = false)
  private LocalDateTime awardedAt;

  protected StudentBadgeEntity() {}

  public StudentBadgeEntity(UUID id, UUID studentId, UUID badgeId, LocalDateTime awardedAt) {
    this.id = id;
    this.studentId = studentId;
    this.badgeId = badgeId;
    this.awardedAt = awardedAt;
  }

  public static StudentBadgeEntity fromDomain(StudentBadge domain) {
    return new StudentBadgeEntity(
        domain.id(), domain.studentId(), domain.badgeId(), domain.awardedAt());
  }

  public StudentBadge toDomain() {
    return new StudentBadge(id, studentId, badgeId, awardedAt);
  }

  public UUID getBadgeId() {
    return badgeId;
  }
}
