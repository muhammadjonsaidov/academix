package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.FileType;
import uz.academixai.domain.TeacherSyllabus;

/**
 * JPA mapping for {@code teacher_syllabuses} (TZ §1.17, migration V15 — missing from
 * backend_tdd.md's DDL, see ROADMAP.md Sprint 3). Maps to/from {@link TeacherSyllabus}.
 */
@Entity
@Table(name = "teacher_syllabuses")
public class TeacherSyllabusEntity {

  @Id private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "class_id", nullable = false)
  private UUID classId;

  @Column(nullable = false)
  private String title;

  @Column(name = "file_url", nullable = false)
  private String fileUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "file_type", nullable = false)
  private FileType fileType;

  @Column(name = "extracted_content")
  private String extractedContent;

  @Column(name = "is_processed", nullable = false)
  private boolean isProcessed;

  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt;

  protected TeacherSyllabusEntity() {}

  public TeacherSyllabusEntity(
      UUID id,
      UUID teacherId,
      UUID subjectId,
      UUID classId,
      String title,
      String fileUrl,
      FileType fileType,
      String extractedContent,
      boolean isProcessed,
      LocalDateTime uploadedAt) {
    this.id = id;
    this.teacherId = teacherId;
    this.subjectId = subjectId;
    this.classId = classId;
    this.title = title;
    this.fileUrl = fileUrl;
    this.fileType = fileType;
    this.extractedContent = extractedContent;
    this.isProcessed = isProcessed;
    this.uploadedAt = uploadedAt;
  }

  public static TeacherSyllabusEntity fromDomain(TeacherSyllabus domain) {
    return new TeacherSyllabusEntity(
        domain.id(),
        domain.teacherId(),
        domain.subjectId(),
        domain.classId(),
        domain.title(),
        domain.fileUrl(),
        domain.fileType(),
        domain.extractedContent(),
        domain.isProcessed(),
        domain.uploadedAt());
  }

  public TeacherSyllabus toDomain() {
    return new TeacherSyllabus(
        id,
        teacherId,
        subjectId,
        classId,
        title,
        fileUrl,
        fileType,
        extractedContent,
        isProcessed,
        uploadedAt);
  }

  public UUID getId() {
    return id;
  }

  public UUID getTeacherId() {
    return teacherId;
  }
}
