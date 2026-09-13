package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import uz.academixai.school.application.TeacherStudent;

public record TeacherStudentResponse(
    UUID id, String firstName, String lastName, String studentNumber) {

  public static TeacherStudentResponse from(TeacherStudent student) {
    return new TeacherStudentResponse(
        student.id(), student.firstName(), student.lastName(), student.studentNumber());
  }
}
