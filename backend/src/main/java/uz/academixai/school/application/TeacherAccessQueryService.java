package uz.academixai.school.application;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.academixai.domain.SchoolClass;
import uz.academixai.domain.Subject;
import uz.academixai.school.application.port.in.TeacherAccess;
import uz.academixai.school.application.port.out.TeacherAccessReadRepository;
import uz.academixai.school.application.port.out.TeacherAssignmentQuery;
import uz.academixai.shared.error.ApiException;

/** Teacher-facing School read queries and their membership authorization policy. */
@Service
public class TeacherAccessQueryService implements TeacherAccess {

  private final TeacherAssignmentQuery assignments;
  private final TeacherAccessReadRepository readRepository;

  public TeacherAccessQueryService(
      TeacherAssignmentQuery assignments, TeacherAccessReadRepository readRepository) {
    this.assignments = assignments;
    this.readRepository = readRepository;
  }

  @Override
  public List<SchoolClass> myClasses(UUID schoolId, UUID teacherId) {
    return readRepository.findClassesById(assignments.classIds(schoolId, teacherId));
  }

  @Override
  public List<Subject> mySubjects(UUID schoolId, UUID teacherId) {
    return readRepository.findSubjectsById(assignments.subjectIds(schoolId, teacherId));
  }

  @Override
  public List<TeacherStudent> classStudents(UUID schoolId, UUID teacherId, UUID classId) {
    requireAssignedToClass(schoolId, teacherId, classId);
    return readRepository.findStudents(schoolId, classId);
  }

  @Override
  public void requireAssignedToClass(UUID schoolId, UUID teacherId, UUID classId) {
    if (!assignments.isAssignedToClass(schoolId, teacherId, classId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_NOT_ASSIGNED",
          "Siz bu sinfga biriktirilmagansiz.",
          "Admin bilan bog'laning.");
    }
  }

  @Override
  public void requireAssignedToClassAndSubject(
      UUID schoolId, UUID teacherId, UUID classId, UUID subjectId) {
    if (!assignments.isAssignedToClassAndSubject(schoolId, teacherId, classId, subjectId)) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "ERR_NOT_ASSIGNED",
          "Siz bu sinf/fanga biriktirilmagansiz.",
          "Admin bilan bog'laning.");
    }
  }
}
