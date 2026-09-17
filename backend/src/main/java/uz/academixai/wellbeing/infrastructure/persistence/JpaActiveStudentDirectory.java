package uz.academixai.wellbeing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.wellbeing.application.port.out.ActiveStudentDirectory;

/** Existing student-profile table adapter for the Wellbeing analysis population. */
@Repository
public class JpaActiveStudentDirectory implements ActiveStudentDirectory {

  private final StudentProfileRepository students;

  public JpaActiveStudentDirectory(StudentProfileRepository students) {
    this.students = students;
  }

  @Override
  public List<Student> findAllActive() {
    return students.findAll().stream()
        .filter(StudentProfileEntity::isActive)
        .map(profile -> new Student(profile.getUserId(), profile.getSchoolId()))
        .toList();
  }

  @Override
  public Optional<UUID> findSchoolId(UUID studentId) {
    return students.findByUserId(studentId).map(StudentProfileEntity::getSchoolId);
  }
}
