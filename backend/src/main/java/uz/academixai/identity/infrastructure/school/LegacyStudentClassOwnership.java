package uz.academixai.identity.infrastructure.school;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.identity.application.port.out.StudentClassOwnership;
import uz.academixai.infrastructure.persistence.SchoolClassEntity;
import uz.academixai.infrastructure.persistence.SchoolClassRepository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;

/**
 * Adapter for {@link StudentClassOwnership} over the legacy class and student-profile repositories.
 */
@Component
public class LegacyStudentClassOwnership implements StudentClassOwnership {

  private final StudentProfileRepository students;
  private final SchoolClassRepository classes;

  public LegacyStudentClassOwnership(
      StudentProfileRepository students, SchoolClassRepository classes) {
    this.students = students;
    this.classes = classes;
  }

  @Override
  public Optional<UUID> classIdOf(UUID schoolId, UUID studentId) {
    return students
        .findByUserIdAndSchoolId(studentId, schoolId)
        .map(StudentProfileEntity::getClassId);
  }

  @Override
  public Optional<UUID> classTeacherOf(UUID classId) {
    return classes.findById(classId).map(SchoolClassEntity::getClassTeacherId);
  }
}
