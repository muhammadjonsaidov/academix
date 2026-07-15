package uz.academixai.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
import uz.academixai.domain.Role;

/**
 * Issues/verifies self-signed JWTs (jjwt) — plain Spring Security, not spring-security-oauth2, per
 * CLAUDE.md "Supporting tooling". Access: 15min. Refresh: 7 days, revocable via {@link
 * RefreshTokenStore}.
 *
 * <p>Signing key is the <b>raw</b> JWT_SECRET bytes, not a hash of it — this must match frontend
 * proxy.ts's {@code new TextEncoder().encode(process.env.JWT_SECRET)} exactly
 * (academix_frontend_tdd.md §6.2), since both sides verify the same httpOnly cookie JWT. A SHA-256
 * pre-hash was tried first and would have silently broken cross-verification — caught before it
 * shipped by comparing against the frontend TDD's actual code, not by a failing test.
 *
 * <p>Algorithm is explicitly pinned to HS256 — {@code Keys.hmacShaKeyFor()} auto-selects the
 * strongest algorithm the key length supports (confirmed by a real token coming back HS384, since
 * the local-dev placeholder secret is 51 bytes), which would silently shift between HS256/384/512
 * if JWT_SECRET's length ever changes. Both sides verify the same HMAC key material regardless of
 * sub-algorithm, so this isn't a hard bug either way — jose does not need pinning to match — but
 * leaving it implicit is needless unpredictability. HS256 requires >= 256-bit (32-byte) keys;
 * JwtProperties validates this at startup instead of failing cryptically on the first token
 * operation.
 */
@Service
public class JwtService {

  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_SCHOOL_ID = "schoolId";

  private final SecretKey signingKey;
  private final JwtProperties properties;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String issueAccessToken(UUID userId, Role role, UUID schoolId) {
    Instant now = Instant.now();
    var builder =
        Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_ROLE, role.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.accessTokenTtlMinutes(), ChronoUnit.MINUTES)))
            .signWith(signingKey, Jwts.SIG.HS256);
    if (schoolId != null) {
      builder.claim(CLAIM_SCHOOL_ID, schoolId.toString());
    }
    return builder.compact();
  }

  public record RefreshToken(String token, UUID jti) {}

  public RefreshToken issueRefreshToken(UUID userId) {
    Instant now = Instant.now();
    UUID jti = UUID.randomUUID();
    String token =
        Jwts.builder()
            .subject(userId.toString())
            .id(jti.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.refreshTokenTtlDays(), ChronoUnit.DAYS)))
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact();
    return new RefreshToken(token, jti);
  }

  public record AccessTokenClaims(UUID userId, Role role, UUID schoolId) {}

  public AccessTokenClaims parseAccessToken(String token) {
    Claims claims = parse(token);
    String schoolIdClaim = claims.get(CLAIM_SCHOOL_ID, String.class);
    return new AccessTokenClaims(
        UUID.fromString(claims.getSubject()),
        Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
        schoolIdClaim == null ? null : UUID.fromString(schoolIdClaim));
  }

  public record RefreshTokenClaims(UUID userId, UUID jti) {}

  public RefreshTokenClaims parseRefreshToken(String token) {
    Claims claims = parse(token);
    return new RefreshTokenClaims(
        UUID.fromString(claims.getSubject()), UUID.fromString(claims.getId()));
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public long accessTokenTtlSeconds() {
    return properties.accessTokenTtlMinutes() * 60;
  }

  public long refreshTokenTtlSeconds() {
    return properties.refreshTokenTtlDays() * 24 * 60 * 60;
  }
}
