package uz.academixai.interfaces.web.teacher;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.learning.application.port.in.UniqueTaskReview;

/**
 * academix_tz.md §2.3 "Unique vazifalarni ko'rish va tasdiqlash" — exact contract, don't drift
 * path/shape from spec.
 */
@RestController
@RequestMapping("/api/v1/teacher/homework/{assignmentId}/unique-tasks")
@PreAuthorize("hasRole('TEACHER')")
public class UniqueTaskController {

  private final UniqueTaskReview reviewService;

  public UniqueTaskController(UniqueTaskReview reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping
  public List<UniqueTaskResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID assignmentId) {
    return reviewService.list(principal.schoolId(), principal.userId(), assignmentId).stream()
        .map(UniqueTaskResponse::from)
        .toList();
  }

  @PutMapping("/{taskId}/approve")
  public ResponseEntity<Void> approve(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID assignmentId,
      @PathVariable UUID taskId) {
    reviewService.approve(principal.schoolId(), principal.userId(), assignmentId, taskId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{taskId}")
  public ResponseEntity<Void> edit(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID assignmentId,
      @PathVariable UUID taskId,
      @Valid @RequestBody EditUniqueTaskRequest request) {
    reviewService.editContent(
        principal.schoolId(), principal.userId(), assignmentId, taskId, request.taskContent());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/approve-all")
  public ResponseEntity<Void> approveAll(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID assignmentId) {
    reviewService.approveAll(principal.schoolId(), principal.userId(), assignmentId);
    return ResponseEntity.noContent().build();
  }
}
