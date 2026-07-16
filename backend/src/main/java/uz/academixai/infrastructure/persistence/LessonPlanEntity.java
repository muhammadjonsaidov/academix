package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.academixai.domain.LessonPlan;
import uz.academixai.domain.LessonPlanContent;

/**
 * JPA mapping for {@code lesson_plans} (TZ §1.18, migration V16 — missing from backend_tdd.md's
 * DDL, see ROADMAP.md Sprint 3). Maps to/from {@link LessonPlan}.
 */
@Entity
@Table(name = "lesson_plans")
public class LessonPlanEntity {

  @Id private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "class_id", nullable = false)
  private UUID classId;

  @Column(name = "syllabus_id")
  private UUID syllabusId;

  @Column(nullable = false)
  private String topic;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ai_generated_plan")
  private LessonPlanContent aiGeneratedPlan;

  // Plain edited text, not the structured LessonPlanContent shape (matches TZ §2.3's PUT body
  // example, a bare string) — plain TEXT column (V18), not jsonb. A real "invalid input syntax
  // for type json" failure on the first non-null value written here (V16 originally made this
  // jsonb like AIFeedbackEntity.highlightedErrors, but that field is always null in practice so
  // the bug was latent) confirmed jsonb requires its raw content to already be valid JSON —
  // Postgres rejects a bare string like "edited". See V18's migration comment.
  @Column(name = "teacher_edited_plan")
  private String teacherEditedPlan;

  @Column(name = "is_approved", nullable = false)
  private boolean isApproved;

  @Column(name = "lesson_date", nullable = false)
  private LocalDate lessonDate;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  protected LessonPlanEntity() {}

  public LessonPlanEntity(
      UUID id,
      UUID teacherId,
      UUID subjectId,
      UUID classId,
      UUID syllabusId,
      String topic,
      LessonPlanContent aiGeneratedPlan,
      String teacherEditedPlan,
      boolean isApproved,
      LocalDate lessonDate,
      LocalDateTime createdAt) {
    this.id = id;
    this.teacherId = teacherId;
    this.subjectId = subjectId;
    this.classId = classId;
    this.syllabusId = syllabusId;
    this.topic = topic;
    this.aiGeneratedPlan = aiGeneratedPlan;
    this.teacherEditedPlan = teacherEditedPlan;
    this.isApproved = isApproved;
    this.lessonDate = lessonDate;
    this.createdAt = createdAt;
  }

  public static LessonPlanEntity fromDomain(LessonPlan domain) {
    return new LessonPlanEntity(
        domain.id(),
        domain.teacherId(),
        domain.subjectId(),
        domain.classId(),
        domain.syllabusId(),
        domain.topic(),
        domain.aiGeneratedPlan(),
        domain.teacherEditedPlan(),
        domain.isApproved(),
        domain.lessonDate(),
        domain.createdAt());
  }

  public LessonPlan toDomain() {
    return new LessonPlan(
        id,
        teacherId,
        subjectId,
        classId,
        syllabusId,
        topic,
        aiGeneratedPlan,
        teacherEditedPlan,
        isApproved,
        lessonDate,
        createdAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getTeacherId() {
    return teacherId;
  }
}
