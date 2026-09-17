package uz.academixai.interfaces.web.teacher;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.GradingCriteriaService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.3 "Baholash mezonlari" — exact contract, don't drift path/shape from spec. */
@RestController
@RequestMapping("/api/v1/teacher/grading-criteria")
@PreAuthorize("hasRole('TEACHER')")
public class GradingCriteriaController {

  private final GradingCriteriaService gradingCriteriaService;

  public GradingCriteriaController(GradingCriteriaService gradingCriteriaService) {
    this.gradingCriteriaService = gradingCriteriaService;
  }

  @GetMapping
  public List<CriteriaItemDto> get(
      @AuthenticationPrincipal AcademixPrincipal principal, @RequestParam UUID subjectId) {
    return gradingCriteriaService.get(principal.userId(), subjectId).stream()
        .map(CriteriaItemDto::from)
        .toList();
  }

  @PutMapping("/{subjectId}")
  public List<CriteriaItemDto> update(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID subjectId,
      @Valid @RequestBody UpdateGradingCriteriaRequest request) {
    var criteria = request.criteria().stream().map(CriteriaItemDto::toDomain).toList();
    return gradingCriteriaService.upsert(principal.userId(), subjectId, criteria).stream()
        .map(CriteriaItemDto::from)
        .toList();
  }
}
