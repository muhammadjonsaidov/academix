package uz.academixai.interfaces.web.parent;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import uz.academixai.application.DataDeletionService;
import uz.academixai.application.ParentDashboardService;
import uz.academixai.application.ParentLinkService;
import uz.academixai.application.ParentProgressService;
import uz.academixai.application.ParentReportService;
import uz.academixai.application.StudentSubmissionService.StudentHomeworkItem;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.reporting.adapter.in.web.ReportResponse;
import uz.academixai.reporting.application.ReportService.ReportDownload;

/** academix_tz.md §2.5 "Parent API" — exact paths, some response shapes deviate. */
@RestController
@RequestMapping("/api/v1/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentController {

  private final DataDeletionService dataDeletionService;
  private final ParentLinkService parentLinkService;
  private final ParentDashboardService parentDashboardService;
  private final ParentProgressService parentProgressService;
  private final ParentReportService parentReportService;

  public ParentController(
      DataDeletionService dataDeletionService,
      ParentLinkService parentLinkService,
      ParentDashboardService parentDashboardService,
      ParentProgressService parentProgressService,
      ParentReportService parentReportService) {
    this.dataDeletionService = dataDeletionService;
    this.parentLinkService = parentLinkService;
    this.parentDashboardService = parentDashboardService;
    this.parentProgressService = parentProgressService;
    this.parentReportService = parentReportService;
  }

  @GetMapping("/children/{studentId}/quarter-report")
  public ReportResponse quarterReport(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    return ReportResponse.from(parentReportService.quarterReport(principal.userId(), studentId));
  }

  @GetMapping("/children/{studentId}/quarter-report/download")
  public ResponseEntity<byte[]> downloadQuarterReport(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    ReportDownload download = parentReportService.download(principal.userId(), studentId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(download.fileName()).build().toString())
        .body(download.content());
  }

  @GetMapping("/dashboard")
  public ParentDashboardResponse dashboard(@AuthenticationPrincipal AcademixPrincipal principal) {
    return ParentDashboardResponse.from(parentDashboardService.dashboard(principal.userId()));
  }

  @GetMapping("/children")
  public List<ChildSummaryResponse> children(@AuthenticationPrincipal AcademixPrincipal principal) {
    return parentDashboardService.children(principal.userId()).stream()
        .map(ChildSummaryResponse::from)
        .toList();
  }

  @GetMapping("/children/{studentId}/overview")
  public ChildOverviewResponse overview(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    return ChildOverviewResponse.from(
        parentDashboardService.overview(principal.userId(), studentId));
  }

  @PostMapping("/children/{studentId}/data-deletion-request")
  public ResponseEntity<DataDeletionRequestResponse> requestDeletion(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    var request = dataDeletionService.requestDeletion(principal.userId(), studentId);
    return ResponseEntity.ok(DataDeletionRequestResponse.from(request));
  }

  @PutMapping("/children/{studentId}/consent/biometric")
  public ResponseEntity<Void> setBiometricConsent(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID studentId,
      @RequestBody SetBiometricConsentRequest request) {
    parentLinkService.setBiometricConsent(principal.userId(), studentId, request.consentGiven());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/children/{studentId}/progress")
  public ParentProgressResponse progress(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    return ParentProgressResponse.from(
        parentProgressService.progress(principal.userId(), studentId));
  }

  @GetMapping("/children/{studentId}/homework")
  public List<StudentHomeworkItem> homework(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    return parentProgressService.homework(principal.userId(), studentId);
  }

  @GetMapping("/children/{studentId}/submissions")
  public List<ParentSubmissionResponse> submissions(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    return parentProgressService.submissions(principal.userId(), studentId).stream()
        .map(ParentSubmissionResponse::from)
        .toList();
  }

  @GetMapping("/children/{studentId}/grades")
  public ParentGradesResponse grades(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    return ParentGradesResponse.from(parentProgressService.grades(principal.userId(), studentId));
  }
}
