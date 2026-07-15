package uz.academixai.interfaces.web.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uz.academixai.application.BulkImportService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.2 "Ommaviy import" — exact contract, don't drift path/shape from the spec. */
@RestController
@RequestMapping("/api/v1/admin/students/bulk-import")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBulkImportController {

  private final BulkImportService bulkImportService;

  public AdminBulkImportController(BulkImportService bulkImportService) {
    this.bulkImportService = bulkImportService;
  }

  @PostMapping("/analyze")
  public ImportAnalyzeResponse analyze(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam("file") MultipartFile file) {
    return bulkImportService.analyze(principal.schoolId(), file);
  }

  @PostMapping("/commit")
  public ImportCommitResponse commit(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody ImportCommitRequest request) {
    return bulkImportService.commit(
        principal.schoolId(),
        request.fileToken(),
        request.columnMapping(),
        request.saveMappingAsTemplate());
  }
}
