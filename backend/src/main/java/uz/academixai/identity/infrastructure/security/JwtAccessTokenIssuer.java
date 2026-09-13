package uz.academixai.identity.infrastructure.security;

import java.util.UUID;
import org.springframework.stereotype.Component;
import uz.academixai.domain.Role;
import uz.academixai.identity.application.port.out.AccessTokenIssuer;
import uz.academixai.infrastructure.security.JwtService;

/** JWT adapter for Identity's token port. JWT types do not leak into the use-case layer. */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final JwtService jwtService;

  public JwtAccessTokenIssuer(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public String issueAccessToken(UUID userId, Role role, UUID schoolId) {
    return jwtService.issueAccessToken(userId, role, schoolId);
  }

  @Override
  public IssuedRefreshToken issueRefreshToken(UUID userId) {
    JwtService.RefreshToken token = jwtService.issueRefreshToken(userId);
    return new IssuedRefreshToken(token.token(), token.jti());
  }

  @Override
  public RefreshTokenClaims parseRefreshToken(String token) {
    JwtService.RefreshTokenClaims claims = jwtService.parseRefreshToken(token);
    return new RefreshTokenClaims(claims.userId(), claims.jti());
  }

  @Override
  public long refreshTokenTtlSeconds() {
    return jwtService.refreshTokenTtlSeconds();
  }
}
