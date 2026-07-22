package uz.academixai.interfaces.web.admin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.persistence.SubjectRepository;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/**
 * No admin-facing subjects catalog was ever built — flagged in an earlier session's audit: subjects
 * are seed/fixture data (no creation endpoint exists per CLAUDE.md's known gaps), but the
 * assignments-linking form (AdminAssignmentController) had no way to look them up, so its subject
 * field was a raw UUID text input instead of a dropdown. This is the minimal read-only endpoint
 * that fixes that — {@link SubjectRepository#findBySchoolIdOrderByName} already existed, unused.
 */
@RestController
@RequestMapping("/api/v1/admin/subjects")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSubjectController {

  private final SubjectRepository subjectRepository;

  public AdminSubjectController(SubjectRepository subjectRepository) {
    this.subjectRepository = subjectRepository;
  }

  @GetMapping
  public List<SubjectResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return subjectRepository.findBySchoolIdOrderByName(principal.schoolId()).stream()
        .map(entity -> SubjectResponse.from(entity.toDomain()))
        .toList();
  }
}
