package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.domain.ClassSubjectTeacher;

public record AssignmentResponse(
    UUID id, UUID teacherId, UUID classId, UUID subjectId, String academicYear) {

  public static AssignmentResponse from(ClassSubjectTeacher domain) {
    return new AssignmentResponse(
        domain.id(),
        domain.teacherId(),
        domain.classId(),
        domain.subjectId(),
        domain.academicYear());
  }
}
