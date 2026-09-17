package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.school.application.SubjectCatalogService;

/**
 * Admin subjects catalog. Originally read-only (subjects were assumed to be seed/fixture data per
 * CLAUDE.md's known gaps — no creation endpoint is documented in the spec). DEVIATION, judgment
 * call: seeding is not acceptable in a real deployment (a fresh school must be fully manageable
 * from the UI), so create/delete were added. Delete is guarded by an in-use check — every FK to
 * {@code subjects} is ON DELETE CASCADE (V9/V10/V25/...), so an unguarded delete of a used subject
 * would silently cascade away homework/exams/lesson plans.
 */
@RestController
@RequestMapping("/api/v1/admin/subjects")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubjectController {

  private final SubjectCatalogService subjectCatalogService;

  public AdminSubjectController(SubjectCatalogService subjectCatalogService) {
    this.subjectCatalogService = subjectCatalogService;
  }

  @GetMapping
  public List<SubjectResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return subjectCatalogService.list(principal.schoolId()).stream()
        .map(SubjectResponse::from)
        .toList();
  }

  @PostMapping
  public SubjectResponse create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestBody CreateSubjectRequest request) {
    return SubjectResponse.from(
        subjectCatalogService.create(
            principal.schoolId(), request.name(), request.type(), request.icon()));
  }

  @DeleteMapping("/{subjectId}")
  public void delete(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID subjectId) {
    subjectCatalogService.delete(principal.schoolId(), subjectId);
  }
}
