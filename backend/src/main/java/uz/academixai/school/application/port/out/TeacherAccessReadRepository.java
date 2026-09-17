package uz.academixai.school.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import uz.academixai.domain.SchoolClass;
import uz.academixai.domain.Subject;
import uz.academixai.school.application.TeacherStudent;

/** Read-side data needed by the teacher access query use cases. */
public interface TeacherAccessReadRepository {

  List<SchoolClass> findClassesById(Collection<UUID> classIds);

  List<Subject> findSubjectsById(Collection<UUID> subjectIds);

  List<TeacherStudent> findStudents(UUID schoolId, UUID classId);
}
