package uz.academixai.interfaces.web.auth;

import java.util.UUID;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;

public record UserSummary(UUID id, String firstName, String lastName, Role role) {

  public static UserSummary from(User user) {
    return new UserSummary(user.id(), user.firstName(), user.lastName(), user.role());
  }
}
