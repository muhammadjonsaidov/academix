package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA mapping for {@code ai_usage_log} (V38 migration) — a write-once, aggregate-read-only log
 * table (admin analytics' dimensional AI usage breakdown). No matching domain record/{@code
 * toDomain()} — nothing ever reads a row back as an object, only aggregate projection queries (see
 * {@code GradeRepository}'s {@code ClassProgressRow}-style pattern), so the usual domain/entity
 * round trip would be pure ceremony here.
 */
@Entity
@Table(name = "ai_usage_log")
public class AiUsageLogEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "class_id", nullable = false)
  private UUID classId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(nullable = false)
  private String category;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  protected AiUsageLogEntity() {}

  public AiUsageLogEntity(
      UUID id,
      UUID schoolId,
      UUID classId,
      UUID subjectId,
      UUID teacherId,
      String category,
      LocalDateTime createdAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.classId = classId;
    this.subjectId = subjectId;
    this.teacherId = teacherId;
    this.category = category;
    this.createdAt = createdAt;
  }
}
