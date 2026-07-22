package uz.academixai.interfaces.web.admin;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.ReportService;
import uz.academixai.application.ReportService.ReportDownload;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.2 "Hisobotlar". */
@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

  private final ReportService reportService;

  public AdminReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @PostMapping("/generate")
  public ReportResponse generate(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody GenerateReportRequest request) {
    return ReportResponse.from(
        reportService.generate(
            principal.schoolId(),
            request.type(),
            request.quarter(),
            request.targetId(),
            principal.userId()));
  }

  @GetMapping
  public List<ReportResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return reportService.list(principal.schoolId()).stream().map(ReportResponse::from).toList();
  }

  @GetMapping("/{reportId}/download")
  public ResponseEntity<byte[]> download(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID reportId) {
    ReportDownload download = reportService.download(principal.schoolId(), reportId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(download.fileName()).build().toString())
        .body(download.content());
  }
}
