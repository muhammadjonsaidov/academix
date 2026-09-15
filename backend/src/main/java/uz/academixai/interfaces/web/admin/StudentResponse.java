package uz.academixai.interfaces.web.admin;

import java.time.LocalDate;
import java.util.UUID;
import uz.academixai.school.application.port.out.StudentStore.StudentRow;

public record StudentResponse(
    UUID id,
    String firstName,
    String lastName,
    String phone,
    UUID classId,
    String studentNumber,
    LocalDate birthDate,
    boolean isActive) {

  public static StudentResponse from(StudentRow row) {
    return new StudentResponse(
        row.userId(),
        row.firstName(),
        row.lastName(),
        row.phone(),
        row.classId(),
        row.studentNumber(),
        row.birthDate(),
        row.isActive());
  }
}
