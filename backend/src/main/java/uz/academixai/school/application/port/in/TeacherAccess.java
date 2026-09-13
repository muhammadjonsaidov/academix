package uz.academixai.school.application.port.in;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.SchoolClass;
import uz.academixai.domain.Subject;
import uz.academixai.school.application.TeacherStudent;

/** Published School API for teacher-scoped queries and assignment authorization. */
public interface TeacherAccess {

  List<SchoolClass> myClasses(UUID schoolId, UUID teacherId);

  List<Subject> mySubjects(UUID schoolId, UUID teacherId);

  List<TeacherStudent> classStudents(UUID schoolId, UUID teacherId, UUID classId);

  void requireAssignedToClass(UUID schoolId, UUID teacherId, UUID classId);

  void requireAssignedToClassAndSubject(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId);
}
