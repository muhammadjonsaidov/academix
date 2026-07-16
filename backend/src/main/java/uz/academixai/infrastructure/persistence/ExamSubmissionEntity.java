package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.ExamSubmission;
import uz.academixai.domain.SubmissionStatus;

/**
 * JPA mapping for {@code exam_submissions} (backend_tdd.md §4.1 table 17). Maps to/from {@link
 * ExamSubmission}.
 */
@Entity
@Table(name = "exam_submissions")
public class ExamSubmissionEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "exam_id", nullable = false)
  private UUID examId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubmissionStatus status;

  @Column(name = "flagged_for_review", nullable = false)
  private boolean flaggedForReview;

  @Column(name = "uploaded_at")
  private LocalDateTime uploadedAt;

  protected ExamSubmissionEntity() {}

  public ExamSubmissionEntity(
      UUID id,
      UUID schoolId,
      UUID examId,
      UUID studentId,
      String imageUrl,
      SubmissionStatus status,
      boolean flaggedForReview,
      LocalDateTime uploadedAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.examId = examId;
    this.studentId = studentId;
    this.imageUrl = imageUrl;
    this.status = status;
    this.flaggedForReview = flaggedForReview;
    this.uploadedAt = uploadedAt;
  }

  public static ExamSubmissionEntity fromDomain(ExamSubmission domain) {
    return new ExamSubmissionEntity(
        domain.id(),
        domain.schoolId(),
        domain.examId(),
        domain.studentId(),
        domain.imageUrl(),
        domain.status(),
        domain.flaggedForReview(),
        domain.uploadedAt());
  }

  public ExamSubmission toDomain() {
    return new ExamSubmission(
        id, schoolId, examId, studentId, imageUrl, status, flaggedForReview, uploadedAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getExamId() {
    return examId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public SubmissionStatus getStatus() {
    return status;
  }

  public boolean isFlaggedForReview() {
    return flaggedForReview;
  }
}
