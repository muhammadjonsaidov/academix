package uz.academixai.identity.infrastructure.security;

import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.identity.application.port.out.RefreshSessionStore;
import uz.academixai.infrastructure.security.RefreshTokenStore;

/** Redis implementation of Identity's revocable refresh-session store. */
@Component
public class RedisRefreshSessionStore implements RefreshSessionStore {

  private final RefreshTokenStore refreshTokenStore;

  public RedisRefreshSessionStore(RefreshTokenStore refreshTokenStore) {
    this.refreshTokenStore = refreshTokenStore;
  }

  @Override
  public void store(UUID jti, UUID userId, Duration ttl) {
    refreshTokenStore.store(jti, userId, ttl);
  }

  @Override
  public boolean consume(UUID jti, UUID expectedUserId) {
    return refreshTokenStore.consume(jti, expectedUserId);
  }

  @Override
  public void revokeAllForUser(UUID userId) {
    refreshTokenStore.revokeAllForUser(userId);
  }
}
