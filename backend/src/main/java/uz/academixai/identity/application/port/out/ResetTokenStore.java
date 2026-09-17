package uz.academixai.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for the one-time password-reset token.
 *
 * <p>Single-use is part of the contract, not an implementation detail: {@link #consume} removes the
 * token as it returns it, so a replayed link can never be redeemed twice.
 */
public interface ResetTokenStore {

  /** Issues a token for the account, valid for the store's configured TTL. */
  String issue(UUID accountId);

  /**
   * Consumes a token, returning the account it was issued to. Empty when the token is unknown,
   * expired, or already used.
   */
  Optional<UUID> consume(String token);
}
