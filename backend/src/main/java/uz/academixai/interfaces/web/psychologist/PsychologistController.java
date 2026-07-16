package uz.academixai.interfaces.web.psychologist;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.PsychologistService;
import uz.academixai.domain.SignalSeverity;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.6 "Psychologist API" — exact paths, some response/request shapes deviate. */
@RestController
@RequestMapping("/api/v1/psychologist")
@PreAuthorize("hasRole('PSYCHOLOGIST')")
public class PsychologistController {

  private final PsychologistService psychologistService;

  public PsychologistController(PsychologistService psychologistService) {
    this.psychologistService = psychologistService;
  }

  @GetMapping("/dashboard")
  public PsychologistDashboardResponse dashboard(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return PsychologistDashboardResponse.from(psychologistService.dashboard(principal.schoolId()));
  }

  @GetMapping("/signals")
  public List<PsychologistSignalListItemResponse> listSignals(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) SignalSeverity severity,
      @RequestParam(required = false) Boolean resolved) {
    return psychologistService.listSignals(principal.schoolId(), severity, resolved).stream()
        .map(PsychologistSignalListItemResponse::from)
        .toList();
  }

  @GetMapping("/signals/{signalId}")
  public PsychologistSignalDetailResponse getSignal(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID signalId) {
    return PsychologistSignalDetailResponse.from(
        psychologistService.getSignalDetail(principal.schoolId(), signalId));
  }

  @PutMapping("/signals/{signalId}/resolve")
  public ResponseEntity<Void> resolve(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID signalId,
      @RequestBody ResolveSignalRequest request) {
    psychologistService.resolve(
        principal.schoolId(), signalId, request.notes(), request.actionTaken());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/signals/{signalId}/mark-manipulation")
  public ResponseEntity<Void> markManipulation(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID signalId) {
    psychologistService.markManipulation(principal.schoolId(), signalId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/watchlist")
  public List<WatchlistStudentResponse> watchlist(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return psychologistService.watchlist(principal.schoolId()).stream()
        .map(WatchlistStudentResponse::from)
        .toList();
  }

  @PostMapping("/watchlist/{studentId}")
  public ResponseEntity<Void> addToWatchlist(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID studentId,
      @RequestBody(required = false) AddToWatchlistRequest request) {
    String reason = request == null ? null : request.reason();
    psychologistService.addToWatchlist(principal.schoolId(), principal.userId(), studentId, reason);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/watchlist/{studentId}")
  public ResponseEntity<Void> removeFromWatchlist(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    psychologistService.removeFromWatchlist(principal.schoolId(), studentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/reports")
  public MonthlyReportResponse monthlyReport(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false, defaultValue = "monthly") String period) {
    return MonthlyReportResponse.from(psychologistService.monthlyReport(principal.schoolId()));
  }
}
