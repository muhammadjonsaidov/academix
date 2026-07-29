package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.User;
import uz.academixai.infrastructure.persistence.ParentStudentLinkRepository.ChildRow;

public record ParentResponse(
    UUID id,
    String firstName,
    String lastName,
    String phone,
    String email,
    boolean isActive,
    List<LinkedChild> children) {

  public record LinkedChild(
      UUID studentId, String firstName, String lastName, String className, String relation) {}

  public static ParentResponse from(User parent, List<ChildRow> children) {
    return new ParentResponse(
        parent.id(),
        parent.firstName(),
        parent.lastName(),
        parent.phone(),
        parent.email(),
        parent.isActive(),
        children.stream()
            .map(
                c ->
                    new LinkedChild(
                        c.getStudentUserId(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getClassName(),
                        c.getRelation()))
            .toList());
  }
}
