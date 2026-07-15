package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import uz.academixai.infrastructure.persistence.StudentProfileRepository.StudentListRow;

public record TeacherStudentResponse(
    UUID id, String firstName, String lastName, String studentNumber) {

  public static TeacherStudentResponse from(StudentListRow row) {
    return new TeacherStudentResponse(
        row.getUserId(), row.getFirstName(), row.getLastName(), row.getStudentNumber());
  }
}
