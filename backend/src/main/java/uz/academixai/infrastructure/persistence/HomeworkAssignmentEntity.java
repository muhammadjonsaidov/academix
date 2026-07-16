package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.AssignmentType;
import uz.academixai.domain.HomeworkAssignment;

/**
 * JPA mapping for {@code homework_assignments} (backend_tdd.md §4.1 table 7). Maps to/from {@link
 * HomeworkAssignment}.
 */
@Entity
@Table(name = "homework_assignments")
public class HomeworkAssignmentEntity {

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

  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AssignmentType type;

  @Column(name = "assigned_at", nullable = false)
  private LocalDateTime assignedAt;

  @Column(name = "deadline_at", nullable = false)
  private LocalDateTime deadlineAt;

  @Column(name = "max_score")
  private int maxScore;

  @Column(name = "is_active")
  private boolean isActive;

  @Column(name = "syllabus_reference")
  private String syllabusReference;

  @Column(name = "ai_generation_prompt")
  private String aiGenerationPrompt;

  @Column(name = "tasks_published", nullable = false)
  private boolean tasksPublished;

  protected HomeworkAssignmentEntity() {}

  public HomeworkAssignmentEntity(
      UUID id,
      UUID schoolId,
      UUID classId,
      UUID subjectId,
      UUID teacherId,
      String title,
      String description,
      AssignmentType type,
      LocalDateTime assignedAt,
      LocalDateTime deadlineAt,
      int maxScore,
      boolean isActive,
      String syllabusReference,
      String aiGenerationPrompt,
      boolean tasksPublished) {
    this.id = id;
    this.schoolId = schoolId;
    this.classId = classId;
    this.subjectId = subjectId;
    this.teacherId = teacherId;
    this.title = title;
    this.description = description;
    this.type = type;
    this.assignedAt = assignedAt;
    this.deadlineAt = deadlineAt;
    this.maxScore = maxScore;
    this.isActive = isActive;
    this.syllabusReference = syllabusReference;
    this.aiGenerationPrompt = aiGenerationPrompt;
    this.tasksPublished = tasksPublished;
  }

  public static HomeworkAssignmentEntity fromDomain(HomeworkAssignment domain) {
    return new HomeworkAssignmentEntity(
        domain.id(),
        domain.schoolId(),
        domain.classId(),
        domain.subjectId(),
        domain.teacherId(),
        domain.title(),
        domain.description(),
        domain.type(),
        domain.assignedAt(),
        domain.deadlineAt(),
        domain.maxScore(),
        domain.isActive(),
        domain.syllabusReference(),
        domain.aiGenerationPrompt(),
        domain.tasksPublished());
  }

  public HomeworkAssignment toDomain() {
    return new HomeworkAssignment(
        id,
        schoolId,
        classId,
        subjectId,
        teacherId,
        title,
        description,
        type,
        assignedAt,
        deadlineAt,
        maxScore,
        isActive,
        syllabusReference,
        aiGenerationPrompt,
        tasksPublished);
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

  public UUID getSubjectId() {
    return subjectId;
  }

  public UUID getTeacherId() {
    return teacherId;
  }

  public int getMaxScore() {
    return maxScore;
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isTasksPublished() {
    return tasksPublished;
  }
}
