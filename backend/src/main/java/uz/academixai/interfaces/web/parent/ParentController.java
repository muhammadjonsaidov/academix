package uz.academixai.interfaces.web.parent;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.DataDeletionService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.5 "Parent API" — exact paths, some response shapes deviate. */
@RestController
@RequestMapping("/api/v1/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentController {

  private final DataDeletionService dataDeletionService;

  public ParentController(DataDeletionService dataDeletionService) {
    this.dataDeletionService = dataDeletionService;
  }

  @PostMapping("/children/{studentId}/data-deletion-request")
  public ResponseEntity<DataDeletionRequestResponse> requestDeletion(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    var request = dataDeletionService.requestDeletion(principal.userId(), studentId);
    return ResponseEntity.ok(DataDeletionRequestResponse.from(request));
  }
}
