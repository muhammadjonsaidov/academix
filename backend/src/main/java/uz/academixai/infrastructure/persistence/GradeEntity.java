package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.Grade;

/** JPA mapping for {@code grades} (backend_tdd.md §4.1 table 11). Maps to/from {@link Grade}. */
@Entity
@Table(name = "grades")
public class GradeEntity {

  @Id private UUID id;

  @Column(name = "submission_id", nullable = false, unique = true)
  private UUID submissionId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(nullable = false)
  private int score;

  @Column(name = "five_point_grade", nullable = false)
  private int fivePointGrade;

  @Column(name = "teacher_comment")
  private String teacherComment;

  @Column(name = "teacher_overrode_ai")
  private boolean teacherOverrodeAI;

  @Column(name = "ai_original_score")
  private float aiOriginalScore;

  @Column(name = "graded_at")
  private LocalDateTime gradedAt;

  protected GradeEntity() {}

  public GradeEntity(
      UUID id,
      UUID submissionId,
      UUID teacherId,
      int score,
      int fivePointGrade,
      String teacherComment,
      boolean teacherOverrodeAI,
      float aiOriginalScore,
      LocalDateTime gradedAt) {
    this.id = id;
    this.submissionId = submissionId;
    this.teacherId = teacherId;
    this.score = score;
    this.fivePointGrade = fivePointGrade;
    this.teacherComment = teacherComment;
    this.teacherOverrodeAI = teacherOverrodeAI;
    this.aiOriginalScore = aiOriginalScore;
    this.gradedAt = gradedAt;
  }

  public static GradeEntity fromDomain(Grade domain) {
    return new GradeEntity(
        domain.id(),
        domain.submissionId(),
        domain.teacherId(),
        domain.score(),
        domain.fivePointGrade(),
        domain.teacherComment(),
        domain.teacherOverrodeAI(),
        domain.aiOriginalScore(),
        domain.gradedAt());
  }

  public Grade toDomain() {
    return new Grade(
        id,
        submissionId,
        teacherId,
        score,
        fivePointGrade,
        teacherComment,
        teacherOverrodeAI,
        aiOriginalScore,
        gradedAt);
  }

  public UUID getSubmissionId() {
    return submissionId;
  }
}
