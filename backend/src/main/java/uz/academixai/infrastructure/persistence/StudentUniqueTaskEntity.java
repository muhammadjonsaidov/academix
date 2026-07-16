package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.StudentUniqueTask;

/**
 * JPA mapping for {@code student_unique_tasks} (backend_tdd.md §4.1 table 8). Maps to/from {@link
 * StudentUniqueTask}.
 */
@Entity
@Table(name = "student_unique_tasks")
public class StudentUniqueTaskEntity {

  @Id private UUID id;

  @Column(name = "assignment_id", nullable = false)
  private UUID assignmentId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "task_content", nullable = false)
  private String taskContent;

  @Column(name = "teacher_approved")
  private boolean teacherApproved;

  @Column(name = "flagged_for_review", nullable = false)
  private boolean flaggedForReview;

  @Column(name = "fallback_to_standard", nullable = false)
  private boolean fallbackToStandard;

  @Column(name = "generated_at")
  private LocalDateTime generatedAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  protected StudentUniqueTaskEntity() {}

  public StudentUniqueTaskEntity(
      UUID id,
      UUID assignmentId,
      UUID studentId,
      String taskContent,
      boolean teacherApproved,
      boolean flaggedForReview,
      boolean fallbackToStandard,
      LocalDateTime generatedAt,
      LocalDateTime approvedAt) {
    this.id = id;
    this.assignmentId = assignmentId;
    this.studentId = studentId;
    this.taskContent = taskContent;
    this.teacherApproved = teacherApproved;
    this.flaggedForReview = flaggedForReview;
    this.fallbackToStandard = fallbackToStandard;
    this.generatedAt = generatedAt;
    this.approvedAt = approvedAt;
  }

  public static StudentUniqueTaskEntity fromDomain(StudentUniqueTask domain) {
    return new StudentUniqueTaskEntity(
        domain.id(),
        domain.assignmentId(),
        domain.studentId(),
        domain.taskContent(),
        domain.teacherApproved(),
        domain.flaggedForReview(),
        domain.fallbackToStandard(),
        domain.generatedAt(),
        domain.approvedAt());
  }

  public StudentUniqueTask toDomain() {
    return new StudentUniqueTask(
        id,
        assignmentId,
        studentId,
        taskContent,
        teacherApproved,
        flaggedForReview,
        fallbackToStandard,
        generatedAt,
        approvedAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getAssignmentId() {
    return assignmentId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public boolean isFlaggedForReview() {
    return flaggedForReview;
  }
}
