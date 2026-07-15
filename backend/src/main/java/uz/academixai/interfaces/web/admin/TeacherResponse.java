package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.domain.User;

public record TeacherResponse(
    UUID id, String firstName, String lastName, String phone, String email, boolean isActive) {

  public static TeacherResponse from(User user) {
    return new TeacherResponse(
        user.id(), user.firstName(), user.lastName(), user.phone(), user.email(), user.isActive());
  }
}
