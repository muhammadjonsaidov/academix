package uz.academixai.application;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.academixai.domain.User;
import uz.academixai.infrastructure.persistence.UserEntity;
import uz.academixai.infrastructure.persistence.UserRepository;
import uz.academixai.infrastructure.security.JwtService;
import uz.academixai.infrastructure.security.RefreshTokenStore;
import uz.academixai.interfaces.web.ApiException;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenStore refreshTokenStore;
  private final SchoolContextResolver schoolContextResolver;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      RefreshTokenStore refreshTokenStore,
      SchoolContextResolver schoolContextResolver) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.refreshTokenStore = refreshTokenStore;
    this.schoolContextResolver = schoolContextResolver;
  }

  public record LoginResult(String accessToken, String refreshToken, User user) {}

  public LoginResult login(String phone, String rawPassword) {
    UserEntity entity =
        userRepository
            .findByPhone(phone)
            .filter(UserEntity::isActive)
            .orElseThrow(AuthService::invalidCredentials);

    if (!passwordEncoder.matches(rawPassword, entity.getPasswordHash())) {
      throw invalidCredentials();
    }

    entity.setLastLoginAt(LocalDateTime.now());
    userRepository.save(entity);

    User user = entity.toDomain();
    UUID schoolId = schoolContextResolver.resolve(user).orElse(null);

    String accessToken = jwtService.issueAccessToken(user.id(), user.role(), schoolId);
    var refresh = jwtService.issueRefreshToken(user.id());
    refreshTokenStore.store(
        refresh.jti(), user.id(), Duration.ofSeconds(jwtService.refreshTokenTtlSeconds()));

    return new LoginResult(accessToken, refresh.token(), user);
  }

  public String refresh(String refreshToken) {
    var claims = parseRefreshOrThrow(refreshToken);

    if (!refreshTokenStore.isValid(claims.jti())) {
      throw expiredToken();
    }

    UserEntity entity =
        userRepository
            .findById(claims.userId())
            .filter(UserEntity::isActive)
            .orElseThrow(AuthService::expiredToken);

    User user = entity.toDomain();
    UUID schoolId = schoolContextResolver.resolve(user).orElse(null);
    return jwtService.issueAccessToken(user.id(), user.role(), schoolId);
  }

  public void logout(UUID userId) {
    refreshTokenStore.revokeAllForUser(userId);
  }

  public void changePassword(UUID userId, String oldPassword, String newPassword) {
    UserEntity entity =
        userRepository
            .findById(userId)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.NOT_FOUND,
                        "ERR_USER_NOT_FOUND",
                        "Foydalanuvchi topilmadi.",
                        "Qaytadan tizimga kiring."));

    if (!passwordEncoder.matches(oldPassword, entity.getPasswordHash())) {
      throw invalidCredentials();
    }

    UserEntity updated =
        new UserEntity(
            entity.getId(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getPhone(),
            entity.getEmail(),
            passwordEncoder.encode(newPassword),
            entity.getRole(),
            entity.isActive(),
            entity.getCreatedAt(),
            entity.getLastLoginAt(),
            entity.getSchoolId());
    userRepository.save(updated);
    refreshTokenStore.revokeAllForUser(userId);
  }

  private JwtService.RefreshTokenClaims parseRefreshOrThrow(String refreshToken) {
    try {
      return jwtService.parseRefreshToken(refreshToken);
    } catch (Exception e) {
      throw expiredToken();
    }
  }

  private static ApiException invalidCredentials() {
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        "ERR_INVALID_CREDENTIALS",
        "Telefon raqam yoki parol noto'g'ri.",
        "Ma'lumotlarni tekshirib qayta urinib ko'ring.");
  }

  private static ApiException expiredToken() {
    // ERR_EXPIRED_TOKEN — academix_backend_tdd.md error-code table
    return new ApiException(
        HttpStatus.UNAUTHORIZED,
        "ERR_EXPIRED_TOKEN",
        "Seans muddati tugadi. Tizimga qayta kiring.",
        "Client refresh token orqali access token olishi kerak.");
  }
}
