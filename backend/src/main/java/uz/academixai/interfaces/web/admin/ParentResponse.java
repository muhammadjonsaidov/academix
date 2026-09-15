package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import uz.academixai.domain.User;
import uz.academixai.family.application.port.out.ParentLinkStore.LinkedChild;

public record ParentResponse(
    UUID id,
    String firstName,
    String lastName,
    String phone,
    String email,
    boolean isActive,
    List<ChildView> children) {

  /**
   * The admin list's own view shape (the API contract), deliberately not Family's {@link
   * LinkedChild} domain-adjacent record.
   */
  public record ChildView(
      UUID studentId, String firstName, String lastName, String className, String relation) {}

  public static ParentResponse from(User parent, List<LinkedChild> children) {
    return new ParentResponse(
        parent.id(),
        parent.firstName(),
        parent.lastName(),
        parent.phone(),
        parent.email(),
        parent.isActive(),
        children.stream()
            .map(
                child ->
                    new ChildView(
                        child.studentUserId(),
                        child.firstName(),
                        child.lastName(),
                        child.className(),
                        child.relation().name()))
            .toList());
  }
}
