package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.DataDeletionService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/**
 * academix_tz.md §2.7 — admin-side only this sprint, see {@link DataDeletionService}'s Javadoc for
 * why the parent-facing creation endpoint isn't built yet.
 */
@RestController
@RequestMapping("/api/v1/admin/data-deletion-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDataDeletionController {

  private final DataDeletionService dataDeletionService;

  public AdminDataDeletionController(DataDeletionService dataDeletionService) {
    this.dataDeletionService = dataDeletionService;
  }

  @GetMapping
  public List<DataDeletionRequestResponse> listPending(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return dataDeletionService.listPending(principal.schoolId()).stream()
        .map(DataDeletionRequestResponse::from)
        .toList();
  }

  @PutMapping("/{id}/approve")
  public DataDeletionRequestResponse approve(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID id) {
    return DataDeletionRequestResponse.from(
        dataDeletionService.approveDataDeletion(principal.schoolId(), id));
  }
}
