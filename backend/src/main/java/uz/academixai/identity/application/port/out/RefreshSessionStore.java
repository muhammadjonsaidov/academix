package uz.academixai.identity.application.port.out;

import java.time.Duration;
import java.util.UUID;

/** Revocable refresh-session persistence, independent of the cache/database implementation. */
public interface RefreshSessionStore {

  void store(UUID jti, UUID userId, Duration ttl);

  /** Atomically consumes a refresh session, preventing concurrent-token replay races. */
  boolean consume(UUID jti, UUID expectedUserId);

  void revokeAllForUser(UUID userId);
}
