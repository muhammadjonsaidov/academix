package uz.academixai.identity.application.port.out;

import java.util.UUID;
import uz.academixai.domain.Role;

/** Outbound port for the token technology used by the identity context. */
public interface AccessTokenIssuer {

  String issueAccessToken(UUID userId, Role role, UUID schoolId);

  IssuedRefreshToken issueRefreshToken(UUID userId);

  RefreshTokenClaims parseRefreshToken(String token);

  long refreshTokenTtlSeconds();

  record IssuedRefreshToken(String token, UUID jti) {}

  record RefreshTokenClaims(UUID userId, UUID jti) {}
}
