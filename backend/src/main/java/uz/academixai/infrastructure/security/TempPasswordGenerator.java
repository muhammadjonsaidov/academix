package uz.academixai.infrastructure.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates a random temp password for admin-created accounts (teacher invite, student create). No
 * delivery channel is wired yet (TZ §2.2 invite/create endpoints don't specify one, and SMS vendor
 * selection was deferred — see CLAUDE.md); the temp password is set on the account so it exists and
 * can be reset later, but isn't sent anywhere. Known gap, not this task's scope.
 */
public final class TempPasswordGenerator {

  private static final SecureRandom RANDOM = new SecureRandom();

  private TempPasswordGenerator() {}

  public static String generate() {
    byte[] bytes = new byte[18];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
