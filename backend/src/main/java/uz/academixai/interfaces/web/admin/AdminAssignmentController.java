package uz.academixai.interfaces.web.admin;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
import uz.academixai.school.application.TeacherAssignmentAdministrationService;

/** academix_tz.md §2.2 "O'qituvchi-Sinf-Fan biriktirish" — exact contract, don't drift. */
@RestController
@RequestMapping("/api/v1/admin/assignments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssignmentController {

  private final TeacherAssignmentAdministrationService assignmentService;

  public AdminAssignmentController(TeacherAssignmentAdministrationService assignmentService) {
    this.assignmentService = assignmentService;
  }

  @GetMapping
  public List<AssignmentResponse> list(@AuthenticationPrincipal AcademixPrincipal principal) {
    return assignmentService.list(principal.schoolId()).stream()
        .map(AssignmentResponse::from)
        .toList();
  }

  @PostMapping
  public ResponseEntity<AssignmentResponse> create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @Valid @RequestBody CreateAssignmentRequest request) {
    var created =
        assignmentService.create(
            principal.schoolId(), request.teacherId(), request.classId(), request.subjectId());
    return ResponseEntity.ok(AssignmentResponse.from(created));
  }

  @DeleteMapping("/{assignmentId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID assignmentId) {
    assignmentService.delete(principal.schoolId(), assignmentId);
    return ResponseEntity.noContent().build();
  }
}
