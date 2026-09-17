package uz.academixai.learning.infrastructure.school;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.StudentProfileEntity;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.learning.application.port.out.ClassStudentQuery;

/** Transitional adapter for School's student-profile read model. */
@Repository
public class JpaClassStudentQuery implements ClassStudentQuery {

  private final StudentProfileRepository students;

  public JpaClassStudentQuery(StudentProfileRepository students) {
    this.students = students;
  }

  @Override
  public List<UUID> studentIds(UUID schoolId, UUID classId) {
    return students.findByClassIdAndSchoolId(classId, schoolId).stream()
        .map(StudentProfileEntity::getUserId)
        .toList();
  }
}
