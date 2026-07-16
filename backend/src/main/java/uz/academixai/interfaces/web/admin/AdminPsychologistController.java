package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.PsychologistManagementService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** Deviation, judgment call — see PsychologistManagementService. Mirrors AdminTeacherController. */
@RestController
@RequestMapping("/api/v1/admin/psychologists")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPsychologistController {

  private final PsychologistManagementService psychologistService;

  public AdminPsychologistController(PsychologistManagementService psychologistService) {
    this.psychologistService = psychologistService;
  }

  @GetMapping
  public List<PsychologistResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return psychologistService.list(principal.schoolId()).stream()
        .map(PsychologistResponse::from)
        .toList();
  }

  @PostMapping("/invite")
  public ResponseEntity<PsychologistResponse> invite(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody InvitePsychologistRequest request) {
    var invited =
        psychologistService.invite(
            principal.schoolId(),
            request.phone(),
            request.firstName(),
            request.lastName(),
            request.email());
    return ResponseEntity.ok(PsychologistResponse.from(invited));
  }

  @PutMapping("/{psychologistId}/activate")
  public PsychologistResponse activate(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID psychologistId) {
    return PsychologistResponse.from(
        psychologistService.activate(principal.schoolId(), psychologistId));
  }

  @PutMapping("/{psychologistId}/deactivate")
  public PsychologistResponse deactivate(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID psychologistId) {
    return PsychologistResponse.from(
        psychologistService.deactivate(principal.schoolId(), psychologistId));
  }
}
