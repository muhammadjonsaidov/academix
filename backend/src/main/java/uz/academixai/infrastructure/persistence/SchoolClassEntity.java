package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.academixai.domain.SchoolClass;

/**
 * JPA mapping for {@code school_classes} (backend_tdd.md §4.1). Maps to/from {@link SchoolClass}.
 */
@Entity
@Table(name = "school_classes")
public class SchoolClassEntity {

  @Id private UUID id;

  @Column(name = "school_id", nullable = false)
  private UUID schoolId;

  @Column(nullable = false)
  private int grade;

  @Column(nullable = false)
  private String letter;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "class_teacher_id")
  private UUID classTeacherId;

  @Column(name = "student_count")
  private int studentCount;

  @Column(name = "academic_year", nullable = false)
  private String academicYear;

  @Column(name = "is_active")
  private boolean isActive = true;

  protected SchoolClassEntity() {}

  public SchoolClassEntity(
      UUID id,
      UUID schoolId,
      int grade,
      String letter,
      String fullName,
      UUID classTeacherId,
      int studentCount,
      String academicYear,
      boolean isActive) {
    this.id = id;
    this.schoolId = schoolId;
    this.grade = grade;
    this.letter = letter;
    this.fullName = fullName;
    this.classTeacherId = classTeacherId;
    this.studentCount = studentCount;
    this.academicYear = academicYear;
    this.isActive = isActive;
  }

  public static SchoolClassEntity fromDomain(SchoolClass schoolClass) {
    return new SchoolClassEntity(
        schoolClass.id(),
        schoolClass.schoolId(),
        schoolClass.grade(),
        schoolClass.letter(),
        schoolClass.fullName(),
        schoolClass.classTeacherId(),
        schoolClass.studentCount(),
        schoolClass.academicYear(),
        schoolClass.isActive());
  }

  public SchoolClass toDomain() {
    return new SchoolClass(
        id,
        schoolId,
        grade,
        letter,
        fullName,
        classTeacherId,
        studentCount,
        academicYear,
        isActive);
  }

  public UUID getId() {
    return id;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public int getGrade() {
    return grade;
  }

  public String getLetter() {
    return letter;
  }

  public String getFullName() {
    return fullName;
  }

  public UUID getClassTeacherId() {
    return classTeacherId;
  }

  public int getStudentCount() {
    return studentCount;
  }

  public String getAcademicYear() {
    return academicYear;
  }

  public boolean isActive() {
    return isActive;
  }
}
