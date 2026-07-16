package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.domain.User;

public record PsychologistResponse(
    UUID id, String firstName, String lastName, String phone, String email, boolean isActive) {

  public static PsychologistResponse from(User user) {
    return new PsychologistResponse(
        user.id(), user.firstName(), user.lastName(), user.phone(), user.email(), user.isActive());
  }
}
