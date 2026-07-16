package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.Exam;

/** JPA mapping for {@code exams} (backend_tdd.md §4.1 table 16). Maps to/from {@link Exam}. */
@Entity
@Table(name = "exams")
public class ExamEntity {

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
  private String title;

  @Column(name = "exam_date", nullable = false)
  private LocalDate examDate;

  @Column(name = "max_score")
  private int maxScore;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  protected ExamEntity() {}

  public ExamEntity(
      UUID id,
      UUID schoolId,
      UUID classId,
      UUID subjectId,
      UUID teacherId,
      String title,
      LocalDate examDate,
      int maxScore,
      LocalDateTime createdAt) {
    this.id = id;
    this.schoolId = schoolId;
    this.classId = classId;
    this.subjectId = subjectId;
    this.teacherId = teacherId;
    this.title = title;
    this.examDate = examDate;
    this.maxScore = maxScore;
    this.createdAt = createdAt;
  }

  public static ExamEntity fromDomain(Exam domain) {
    return new ExamEntity(
        domain.id(),
        domain.schoolId(),
        domain.classId(),
        domain.subjectId(),
        domain.teacherId(),
        domain.title(),
        domain.examDate(),
        domain.maxScore(),
        domain.createdAt());
  }

  public Exam toDomain() {
    return new Exam(
        id, schoolId, classId, subjectId, teacherId, title, examDate, maxScore, createdAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getClassId() {
    return classId;
  }

  public UUID getTeacherId() {
    return teacherId;
  }
}
