package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.HomeworkSubmission;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.domain.SubmissionType;

/**
 * JPA mapping for {@code homework_submissions} (backend_tdd.md §4.1 table 9). Maps to/from {@link
 * HomeworkSubmission}.
 */
@Entity
@Table(name = "homework_submissions")
public class HomeworkSubmissionEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "assignment_id", nullable = false)
  private UUID assignmentId;

  @Column(name = "student_task_id")
  private UUID studentTaskId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubmissionType type;

  @Column(name = "text_content")
  private String textContent;

  @Column(name = "image_url")
  private String imageUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubmissionStatus status;

  @Column(name = "is_late")
  private boolean isLate;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "xp_earned")
  private int xpEarned;

  protected HomeworkSubmissionEntity() {}

  public HomeworkSubmissionEntity(
      UUID id,
      UUID schoolId,
      UUID assignmentId,
      UUID studentTaskId,
      UUID studentId,
      SubmissionType type,
      String textContent,
      String imageUrl,
      SubmissionStatus status,
      boolean isLate,
      LocalDateTime submittedAt,
      int xpEarned) {
    this.id = id;
    this.schoolId = schoolId;
    this.assignmentId = assignmentId;
    this.studentTaskId = studentTaskId;
    this.studentId = studentId;
    this.type = type;
    this.textContent = textContent;
    this.imageUrl = imageUrl;
    this.status = status;
    this.isLate = isLate;
    this.submittedAt = submittedAt;
    this.xpEarned = xpEarned;
  }

  public static HomeworkSubmissionEntity fromDomain(HomeworkSubmission domain) {
    return new HomeworkSubmissionEntity(
        domain.id(),
        domain.schoolId(),
        domain.assignmentId(),
        domain.studentTaskId(),
        domain.studentId(),
        domain.type(),
        domain.textContent(),
        domain.imageUrl(),
        domain.status(),
        domain.isLate(),
        domain.submittedAt(),
        domain.xpEarned());
  }

  public HomeworkSubmission toDomain() {
    return new HomeworkSubmission(
        id,
        schoolId,
        assignmentId,
        studentTaskId,
        studentId,
        type,
        textContent,
        imageUrl,
        status,
        isLate,
        submittedAt,
        xpEarned);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getAssignmentId() {
    return assignmentId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public SubmissionStatus getStatus() {
    return status;
  }

  public String getImageUrl() {
    return imageUrl;
  }
}
