package uz.academixai.interfaces.web.teacher;

import java.util.UUID;
import uz.academixai.domain.Subject;

public record TeacherSubjectResponse(UUID id, String name) {

  public static TeacherSubjectResponse from(Subject domain) {
    return new TeacherSubjectResponse(domain.id(), domain.name());
  }
}
