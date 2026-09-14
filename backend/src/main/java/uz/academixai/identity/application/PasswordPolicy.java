package uz.academixai.identity.application;

import org.springframework.http.HttpStatus;
import uz.academixai.interfaces.web.ApiException;

/** Server-side password rule shared by every account-creation and reset path. */
public final class PasswordPolicy {

  private PasswordPolicy() {}

  public static void requireValid(String password) {
    if (password == null || password.length() < 8) {
      throw invalid("Parol kamida 8 belgidan iborat bo'lishi kerak.");
    }
    if (!password.matches(".*[a-z].*")
        || !password.matches(".*[A-Z].*")
        || !password.matches(".*\\d.*")) {
      throw invalid("Parolda kamida bitta katta harf, kichik harf va raqam bo'lishi kerak.");
    }
  }

  private static ApiException invalid(String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        "ERR_VALIDATION",
        message,
        "Kuchliroq parol kiriting va qayta urinib ko'ring.");
  }
}
