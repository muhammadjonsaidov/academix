package uz.academixai.interfaces.web.admin;

import jakarta.validation.Valid;
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
import uz.academixai.domain.Role;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.school.application.StaffManagementService;

/** Deviation, judgment call — see StaffManagementService. Mirrors AdminTeacherController. */
@RestController
@RequestMapping("/api/v1/admin/psychologists")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPsychologistController {

  private final StaffManagementService staffService;

  public AdminPsychologistController(StaffManagementService staffService) {
    this.staffService = staffService;
  }

  @GetMapping
  public List<PsychologistResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return staffService.list(principal.schoolId(), Role.PSYCHOLOGIST).stream()
        .map(PsychologistResponse::from)
        .toList();
  }

  @PostMapping("/invite")
  public ResponseEntity<PsychologistResponse> invite(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @Valid @RequestBody InvitePsychologistRequest request) {
    var invited =
        staffService.invite(
            principal.schoolId(),
            Role.PSYCHOLOGIST,
            request.phone(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.password());
    return ResponseEntity.ok(PsychologistResponse.from(invited));
  }

  @PutMapping("/{psychologistId}/activate")
  public PsychologistResponse activate(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID psychologistId) {
    return PsychologistResponse.from(
        staffService.activate(principal.schoolId(), Role.PSYCHOLOGIST, psychologistId));
  }

  @PutMapping("/{psychologistId}/deactivate")
  public PsychologistResponse deactivate(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID psychologistId) {
    return PsychologistResponse.from(
        staffService.deactivate(principal.schoolId(), Role.PSYCHOLOGIST, psychologistId));
  }
}
