package uz.academixai.interfaces.web.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.family.application.ParentLinkService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md — POST /admin/parents/link, see ParentLinkService's Javadoc for the deviation. */
@RestController
@RequestMapping("/api/v1/admin/parents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminParentLinkController {

  private final ParentLinkService parentLinkService;

  public AdminParentLinkController(ParentLinkService parentLinkService) {
    this.parentLinkService = parentLinkService;
  }

  @PostMapping("/link")
  public ResponseEntity<ParentLinkResponse> link(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody LinkParentRequest request) {
    var link =
        parentLinkService.link(
            principal.schoolId(), request.parentPhone(), request.studentId(), request.relation());
    return ResponseEntity.ok(ParentLinkResponse.from(link));
  }
}
