package uz.academixai.interfaces.web.auth;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.AuthService;
import uz.academixai.application.PasswordResetService;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.infrastructure.security.JwtService;

/**
 * academix_tz.md §2.1 — exact contract, don't drift path/shape from the spec.
 *
 * <p>Deviation on top of the spec (documented, judgment call): the refresh token is additionally
 * set as its own httpOnly cookie ({@code academix_refresh}, path-scoped to /api/v1/auth) and {@code
 * POST /refresh} falls back to that cookie when the body carries no token. This exists so the
 * frontend can bootstrap a session after a hard page reload — its access token lives only in memory
 * (frontend_tdd.md §5.5, deliberately not localStorage), so without this every F5 forced a fresh
 * login. Same security posture as the existing {@code academix_auth} cookie: httpOnly, Secure,
 * SameSite=Strict — JS never sees either token.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final String COOKIE_NAME = "academix_auth";
  private static final String REFRESH_COOKIE_NAME = "academix_refresh";

  private final AuthService authService;
  private final JwtService jwtService;
  private final PasswordResetService passwordResetService;

  // Cookie Domain attribute. Empty (the default) = host-only cookie, correct for local dev where
  // everything is localhost. In a deploy where frontend and backend live on sibling subdomains of
  // one registrable domain (www.academixai.uz + api.academixai.uz), this must be set to the parent
  // domain (".academixai.uz") — otherwise the academix_auth cookie is host-only to the API origin
  // and proxy.ts (running on the frontend origin) never sees it, breaking the route guard and the
  // F5 session restore. SameSite=Strict still works across sibling subdomains: "site" is the
  // registrable domain, so www→api requests are same-site.
  private final String cookieDomain;

  public AuthController(
      AuthService authService,
      JwtService jwtService,
      PasswordResetService passwordResetService,
      @Value("${academix.cookie-domain:}") String cookieDomain) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.passwordResetService = passwordResetService;
    this.cookieDomain = cookieDomain == null || cookieDomain.isBlank() ? null : cookieDomain;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    var result = authService.login(request.phone(), request.password());
    var body =
        new LoginResponse(
            result.accessToken(), result.refreshToken(), UserSummary.from(result.user()));
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, authCookie(result.accessToken()).toString())
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(body);
  }

  @PostMapping("/refresh")
  public ResponseEntity<RefreshResponse> refresh(
      @RequestBody(required = false) RefreshRequest request,
      @CookieValue(value = REFRESH_COOKIE_NAME, required = false) String refreshCookie) {
    String refreshToken =
        request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
            ? request.refreshToken()
            : refreshCookie;
    String accessToken = authService.refresh(refreshToken);
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
        .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
        .body(new LogoutResponse(true));
  }

  @GetMapping("/profile")
  public ProfileResponse profile(@AuthenticationPrincipal AcademixPrincipal principal) {
    return ProfileResponse.from(authService.profile(principal.userId()));
  }

  @PutMapping("/profile")
  public ProfileResponse updateProfile(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody UpdateProfileRequest request) {
    return ProfileResponse.from(
        authService.updateProfile(
            principal.userId(), request.firstName(), request.lastName(), request.email()));
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

  // Signed httpOnly cookie for the frontend's proxy.ts route-guard (jose jwtVerify) — same JWT as
  // the access token, per academix_tz.md §2.1 / academix_frontend_tdd.md §5.5.
  //
  // DEVIATION from those sections' "same 15-min TTL as the access token": the cookie now lives as
  // long as the refresh token. With a 15-minute maxAge the browser DROPPED this cookie after 15
  // idle minutes, and because academix_refresh is path-scoped to /api/v1/auth it is never sent on
  // a /dashboard/* navigation — so proxy.ts saw no cookie at all, could not know a valid 7-day
  // refresh token still existed, and hard-redirected to /login. The client-side session bootstrap
  // that exists precisely to recover this case never got to run. The JWT inside is unchanged and
  // still expires in 15 minutes: it is worthless against the API either way
  // (JwtAuthenticationFilter
  // rejects it), and proxy.ts treats an expired-but-validly-signed cookie as "let through, the
  // client will refresh" — the guard is UX-only route-flash prevention, never authorization.
  private ResponseCookie authCookie(String accessToken) {
    return ResponseCookie.from(COOKIE_NAME, accessToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .domain(cookieDomain)
        .path("/")
        .maxAge(Duration.ofSeconds(jwtService.refreshTokenTtlSeconds()))
        .build();
  }

  private ResponseCookie clearedAuthCookie() {
    return ResponseCookie.from(COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .domain(cookieDomain)
        .path("/")
        .maxAge(0)
        .build();
  }

  // Path-scoped to the auth endpoints only — the refresh token never rides along on ordinary
  // API requests, it's presented exclusively to /api/v1/auth/refresh.
  private ResponseCookie refreshCookie(String refreshToken) {
    return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .domain(cookieDomain)
        .path("/api/v1/auth")
        .maxAge(Duration.ofSeconds(jwtService.refreshTokenTtlSeconds()))
        .build();
  }

  private ResponseCookie clearedRefreshCookie() {
    return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .domain(cookieDomain)
        .path("/api/v1/auth")
        .maxAge(0)
        .build();
  }
}
