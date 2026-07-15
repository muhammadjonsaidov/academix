package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.domain.SchoolClass;

public record ClassResponse(
    UUID id,
    int grade,
    String letter,
    String fullName,
    UUID classTeacherId,
    int studentCount,
    String academicYear,
    boolean isActive) {

  public static ClassResponse from(SchoolClass schoolClass) {
    return new ClassResponse(
        schoolClass.id(),
        schoolClass.grade(),
        schoolClass.letter(),
        schoolClass.fullName(),
        schoolClass.classTeacherId(),
        schoolClass.studentCount(),
        schoolClass.academicYear(),
        schoolClass.isActive());
  }
}
