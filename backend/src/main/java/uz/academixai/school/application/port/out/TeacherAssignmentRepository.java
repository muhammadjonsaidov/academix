package uz.academixai.school.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uz.academixai.domain.ClassSubjectTeacher;

/** Write-side persistence boundary for School's teacher-class-subject membership aggregate. */
public interface TeacherAssignmentRepository {

  List<ClassSubjectTeacher> findBySchoolId(UUID schoolId);

  Optional<ClassSubjectTeacher> findByIdAndSchoolId(UUID assignmentId, UUID schoolId);

  boolean exists(UUID schoolId, UUID teacherId, UUID classId, UUID subjectId);

  ClassSubjectTeacher save(ClassSubjectTeacher assignment);

  void deleteById(UUID assignmentId);
}
