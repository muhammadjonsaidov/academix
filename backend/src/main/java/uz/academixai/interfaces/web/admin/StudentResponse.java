package uz.academixai.interfaces.web.admin;

import java.time.LocalDate;
import java.util.UUID;
import uz.academixai.infrastructure.persistence.StudentProfileRepository.StudentListRow;

public record StudentResponse(
    UUID id,
    String firstName,
    String lastName,
    String phone,
    UUID classId,
    String studentNumber,
    LocalDate birthDate,
    boolean isActive) {

  public static StudentResponse from(StudentListRow row) {
    return new StudentResponse(
        row.getUserId(),
        row.getFirstName(),
        row.getLastName(),
        row.getPhone(),
        row.getClassId(),
        row.getStudentNumber(),
        row.getBirthDate(),
        row.getIsActive());
  }
}
