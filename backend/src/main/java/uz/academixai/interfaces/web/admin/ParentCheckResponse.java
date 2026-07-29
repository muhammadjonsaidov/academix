package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import uz.academixai.application.ParentManagementService.ParentCheck;

/**
 * Pre-link phone lookup (GET /admin/parents/check?phone=...) so the admin sees whether the number
 * already belongs to someone — and, for an existing parent, which children are already linked —
 * BEFORE calling /link.
 */
public record ParentCheckResponse(
    boolean exists,
    String role,
    UUID userId,
    String firstName,
    String lastName,
    List<ParentResponse.LinkedChild> children) {

  public static ParentCheckResponse from(ParentCheck check) {
    if (!check.exists()) {
      return new ParentCheckResponse(false, null, null, null, null, List.of());
    }
    return new ParentCheckResponse(
        true,
        check.role(),
        check.user().id(),
        check.user().firstName(),
        check.user().lastName(),
        check.children().stream()
            .map(
                c ->
                    new ParentResponse.LinkedChild(
                        c.getStudentUserId(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getClassName(),
                        c.getRelation()))
            .toList());
  }
}
