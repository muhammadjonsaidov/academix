package uz.academixai.interfaces.web.auth;

import java.util.UUID;
import uz.academixai.domain.Role;
import uz.academixai.domain.User;
import uz.academixai.identity.domain.Account;

/** Deviation — no /auth/profile shape exists in academix_tz.md §2.1 (self-service settings UI). */
public record ProfileResponse(
    UUID id, String firstName, String lastName, String phone, String email, Role role) {

  public static ProfileResponse from(User user) {
    return new ProfileResponse(
        user.id(), user.firstName(), user.lastName(), user.phone(), user.email(), user.role());
  }

  public static ProfileResponse from(Account account) {
    return new ProfileResponse(
        account.id(),
        account.firstName(),
        account.lastName(),
        account.phone(),
        account.email(),
        account.role());
  }
}
