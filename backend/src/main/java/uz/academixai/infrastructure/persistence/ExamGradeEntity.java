package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.ExamGrade;

/** JPA mapping for {@code exam_grades} (see ExamGrade.java for the deviation note). */
@Entity
@Table(name = "exam_grades")
public class ExamGradeEntity {

  @Id private UUID id;

  @Column(name = "exam_submission_id", nullable = false, unique = true)
  private UUID examSubmissionId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(nullable = false)
  private int score;

  @Column(name = "five_point_grade", nullable = false)
  private int fivePointGrade;

  @Column(name = "teacher_comment")
  private String teacherComment;

  @Column(name = "graded_at")
  private LocalDateTime gradedAt;

  protected ExamGradeEntity() {}

  public ExamGradeEntity(
      UUID id,
      UUID examSubmissionId,
      UUID teacherId,
      int score,
      int fivePointGrade,
      String teacherComment,
      LocalDateTime gradedAt) {
    this.id = id;
    this.examSubmissionId = examSubmissionId;
    this.teacherId = teacherId;
    this.score = score;
    this.fivePointGrade = fivePointGrade;
    this.teacherComment = teacherComment;
    this.gradedAt = gradedAt;
  }

  public static ExamGradeEntity fromDomain(ExamGrade domain) {
    return new ExamGradeEntity(
        domain.id(),
        domain.examSubmissionId(),
        domain.teacherId(),
        domain.score(),
        domain.fivePointGrade(),
        domain.teacherComment(),
        domain.gradedAt());
  }

  public ExamGrade toDomain() {
    return new ExamGrade(
        id, examSubmissionId, teacherId, score, fivePointGrade, teacherComment, gradedAt);
  }

  public UUID getExamSubmissionId() {
    return examSubmissionId;
  }
}
