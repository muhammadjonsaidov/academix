package uz.academixai.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import uz.academixai.domain.StudentProfile;

/**
 * JPA mapping for {@code student_profiles} (backend_tdd.md §4.1). Maps to/from {@link
 * StudentProfile}.
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfileEntity {

  @Id private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "class_id")
  private UUID classId;

  @Column(name = "school_id")
  private UUID schoolId;

  @Column(name = "student_number")
  private String studentNumber;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "total_xp")
  private int totalXp;

  @Column(name = "current_streak")
  private int currentStreak;

  @Column(name = "max_streak")
  private int maxStreak;

  @Column(name = "last_submission_date")
  private LocalDate lastSubmissionDate;

  @Column(name = "is_active")
  private boolean isActive = true;

  protected StudentProfileEntity() {}

  public StudentProfileEntity(
      UUID id,
      UUID userId,
      UUID classId,
      UUID schoolId,
      String studentNumber,
      LocalDate birthDate,
      int totalXp,
      int currentStreak,
      int maxStreak,
      LocalDate lastSubmissionDate,
      boolean isActive) {
    this.id = id;
    this.userId = userId;
    this.classId = classId;
    this.schoolId = schoolId;
    this.studentNumber = studentNumber;
    this.birthDate = birthDate;
    this.totalXp = totalXp;
    this.currentStreak = currentStreak;
    this.maxStreak = maxStreak;
    this.lastSubmissionDate = lastSubmissionDate;
    this.isActive = isActive;
  }

  public static StudentProfileEntity fromDomain(StudentProfile profile) {
    return new StudentProfileEntity(
        profile.id(),
        profile.userId(),
        profile.classId(),
        profile.schoolId(),
        profile.studentNumber(),
        profile.birthDate(),
        profile.totalXp(),
        profile.currentStreak(),
        profile.maxStreak(),
        profile.lastSubmissionDate(),
        profile.isActive());
  }

  public StudentProfile toDomain() {
    return new StudentProfile(
        id,
        userId,
        classId,
        schoolId,
        studentNumber,
        birthDate,
        totalXp,
        currentStreak,
        maxStreak,
        lastSubmissionDate,
        isActive);
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getClassId() {
    return classId;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getStudentNumber() {
    return studentNumber;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public boolean isActive() {
    return isActive;
  }
}
