package uz.academixai.interfaces.web.onboarding;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.identity.application.AuthenticationService;
import uz.academixai.interfaces.web.auth.LoginResponse;
import uz.academixai.interfaces.web.auth.UserSummary;
import uz.academixai.onboarding.application.InitialSetupService;

/** Boundary for provisioning the very first school administrator. */
@RestController
@RequestMapping("/api/v1/onboarding")
public class InitialSetupController {

  private final InitialSetupService setupService;
  private final AuthenticationService authenticationService;

  public InitialSetupController(
      InitialSetupService setupService, AuthenticationService authenticationService) {
    this.setupService = setupService;
    this.authenticationService = authenticationService;
  }

  @GetMapping("/status")
  public InitialSetupStatusResponse status() {
    return new InitialSetupStatusResponse(setupService.isAvailable());
  }

  @PostMapping("/initial-setup")
  public ResponseEntity<LoginResponse> initialSetup(@RequestBody InitialSetupRequest request) {
    setupService.createFirstAdministrator(
        request.firstName(),
        request.lastName(),
        request.phone(),
        request.email(),
        request.password(),
        request.schoolName(),
        request.region(),
        request.district(),
        request.address());
    var login = authenticationService.login(request.phone().trim(), request.password());
    // The auth controller owns the secure cookie policy; the browser can use this access token
    // immediately and receives normal refresh cookies on its first standard login.
    return ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(new LoginResponse(login.accessToken(), UserSummary.from(login.account())));
  }
}
