package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import uz.academixai.domain.SchoolClass;

public record TeacherClassResponse(UUID id, String fullName, int studentCount) {

  public static TeacherClassResponse from(SchoolClass domain) {
    return new TeacherClassResponse(domain.id(), domain.fullName(), domain.studentCount());
  }
}
