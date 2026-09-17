package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.wellbeing.application.TeacherPsychologyService;

/** academix_tz.md §2.3 "Psixologik signallar" — exact paths, response shape is a deviation. */
@RestController
@RequestMapping("/api/v1/teacher/psychological-signals")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherPsychologicalSignalController {

  private final TeacherPsychologyService psychologyService;

  public TeacherPsychologicalSignalController(TeacherPsychologyService psychologyService) {
    this.psychologyService = psychologyService;
  }

  @GetMapping
  public List<TeacherPsychologicalSignalResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) SignalSeverity severity) {
    return psychologyService.list(principal.schoolId(), principal.userId(), severity).stream()
        .map(TeacherPsychologicalSignalResponse::from)
        .toList();
  }

  @GetMapping("/{signalId}")
  public TeacherPsychologicalSignalResponse get(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID signalId) {
    return TeacherPsychologicalSignalResponse.from(
        psychologyService.get(principal.schoolId(), principal.userId(), signalId));
  }

  @PutMapping("/{signalId}/resolve")
  public ResponseEntity<Void> resolve(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID signalId) {
    psychologyService.resolve(principal.schoolId(), principal.userId(), signalId);
    return ResponseEntity.noContent().build();
  }
}
