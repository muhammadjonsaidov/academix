package uz.academixai.learning.infrastructure.school;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import uz.academixai.infrastructure.persistence.StudentProfileRepository;
import uz.academixai.learning.application.port.out.ClassSizeQuery;

/** Transitional adapter for School's student-profile read model. */
@Repository
public class JpaClassSizeQuery implements ClassSizeQuery {

  private final StudentProfileRepository students;

  public JpaClassSizeQuery(StudentProfileRepository students) {
    this.students = students;
  }

  @Override
  public int activeStudentCount(UUID schoolId, UUID classId) {
    return students.countByClassIdAndSchoolId(classId, schoolId);
  }
}
