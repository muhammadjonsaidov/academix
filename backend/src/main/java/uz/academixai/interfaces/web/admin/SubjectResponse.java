package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.domain.Subject;

public record SubjectResponse(UUID id, String name, String type, String icon) {
  public static SubjectResponse from(Subject subject) {
    return new SubjectResponse(subject.id(), subject.name(), subject.type().name(), subject.icon());
  }
}
