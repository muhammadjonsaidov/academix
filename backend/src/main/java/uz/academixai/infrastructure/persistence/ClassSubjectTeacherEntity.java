package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.academixai.domain.ClassSubjectTeacher;

/**
 * JPA mapping for {@code class_subject_teachers} (backend_tdd.md §4.1 table 6.1). Maps to/from
 * {@link ClassSubjectTeacher}.
 */
@Entity
@Table(name = "class_subject_teachers")
public class ClassSubjectTeacherEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(name = "class_id", nullable = false)
  private UUID classId;

  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "academic_year", nullable = false)
  private String academicYear;

  protected ClassSubjectTeacherEntity() {}

  public ClassSubjectTeacherEntity(
      UUID id, UUID schoolId, UUID classId, UUID subjectId, UUID teacherId, String academicYear) {
    this.id = id;
    this.schoolId = schoolId;
    this.classId = classId;
    this.subjectId = subjectId;
    this.teacherId = teacherId;
    this.academicYear = academicYear;
  }

  public static ClassSubjectTeacherEntity fromDomain(ClassSubjectTeacher domain) {
    return new ClassSubjectTeacherEntity(
        domain.id(),
        domain.schoolId(),
        domain.classId(),
        domain.subjectId(),
        domain.teacherId(),
        domain.academicYear());
  }

  public ClassSubjectTeacher toDomain() {
    return new ClassSubjectTeacher(id, schoolId, classId, subjectId, teacherId, academicYear);
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
}
