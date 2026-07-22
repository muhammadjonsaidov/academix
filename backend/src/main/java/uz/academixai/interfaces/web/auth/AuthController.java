package uz.academixai.interfaces.web.auth;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.AuthService;
import uz.academixai.application.PasswordResetService;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.infrastructure.security.JwtService;

/** academix_tz.md §2.1 — exact contract, don't drift path/shape from the spec. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final String COOKIE_NAME = "academix_auth";

  private final AuthService authService;
  private final JwtService jwtService;
  private final PasswordResetService passwordResetService;

  public AuthController(
      AuthService authService, JwtService jwtService, PasswordResetService passwordResetService) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.passwordResetService = passwordResetService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    var result = authService.login(request.phone(), request.password());
    var body =
        new LoginResponse(
            result.accessToken(), result.refreshToken(), UserSummary.from(result.user()));
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, authCookie(result.accessToken()).toString())
        .body(body);
  }

  @PostMapping("/refresh")
  public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest request) {
    String accessToken = authService.refresh(request.refreshToken());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, authCookie(accessToken).toString())
        .body(new RefreshResponse(accessToken));
  }

  @PostMapping("/logout")
  public ResponseEntity<LogoutResponse> logout(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    authService.logout(principal.userId());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, clearedAuthCookie().toString())
        .body(new LogoutResponse(true));
  }

  @PutMapping("/change-password")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody ChangePasswordRequest request) {
    authService.changePassword(principal.userId(), request.oldPassword(), request.newPassword());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/forgot-password")
  public ForgotPasswordResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
    passwordResetService.forgotPassword(request.phone());
    // Always the same response regardless of outcome — see PasswordResetService's Javadoc
    // (anti-enumeration: a caller can't tell "no account," "no email on file," or "sent" apart).
    return new ForgotPasswordResponse(
        true,
        "Agar hisobingiz mavjud bo'lsa va email kiritilgan bo'lsa, tiklash havolasi yuborildi.");
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword());
    return ResponseEntity.ok().build();
  }

  // Signed httpOnly cookie for the frontend's proxy.ts route-guard (jose jwtVerify) — same JWT,
  // same TTL as the access token, per academix_tz.md §2.1 / academix_frontend_tdd.md §5.5.
  private ResponseCookie authCookie(String accessToken) {
    return ResponseCookie.from(COOKIE_NAME, accessToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/")
        .maxAge(Duration.ofSeconds(jwtService.accessTokenTtlSeconds()))
        .build();
  }

  private ResponseCookie clearedAuthCookie() {
    return ResponseCookie.from(COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/")
        .maxAge(0)
        .build();
  }
}
