package uz.academixai.interfaces.web.teacher;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.learning.application.port.in.HomeworkManagement;
import uz.academixai.learning.application.port.in.UniqueTaskReview;

/**
 * academix_tz.md §2.3 "Uy vazifasi yaratish" — exact contract, don't drift path/shape from spec.
 */
@RestController
@RequestMapping("/api/v1/teacher/homework")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherHomeworkController {

  private final HomeworkManagement homeworkService;
  private final UniqueTaskReview uniqueTaskReviewService;

  public TeacherHomeworkController(
      HomeworkManagement homeworkService, UniqueTaskReview uniqueTaskReviewService) {
    this.homeworkService = homeworkService;
    this.uniqueTaskReviewService = uniqueTaskReviewService;
  }

  @PostMapping
  public ResponseEntity<HomeworkResponse> create(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @Valid @RequestBody CreateHomeworkRequest request) {
    var created =
        homeworkService.create(
            principal.schoolId(),
            principal.userId(),
            request.classId(),
            request.subjectId(),
            request.title(),
            request.description(),
            request.type(),
            request.deadlineAt(),
            request.syllabusReference(),
            request.maxScore());
    return ResponseEntity.ok(HomeworkResponse.from(created));
  }

  @GetMapping
  public List<HomeworkResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID classId,
      @RequestParam(required = false) UUID subjectId) {
    return homeworkService
        .list(principal.schoolId(), principal.userId(), classId, subjectId)
        .stream()
        .map(HomeworkResponse::from)
        .toList();
  }

  @GetMapping("/{assignmentId}")
  public HomeworkResponse get(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID assignmentId) {
    return HomeworkResponse.from(homeworkService.get(principal.schoolId(), assignmentId));
  }

  @PutMapping("/{assignmentId}")
  public HomeworkResponse update(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID assignmentId,
      @Valid @RequestBody UpdateHomeworkRequest request) {
    var updated =
        homeworkService.update(
            principal.schoolId(),
            principal.userId(),
            assignmentId,
            request.title(),
            request.description(),
            request.deadlineAt(),
            request.syllabusReference(),
            request.maxScore());
    return HomeworkResponse.from(updated);
  }

  @DeleteMapping("/{assignmentId}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID assignmentId) {
    homeworkService.delete(principal.schoolId(), principal.userId(), assignmentId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{assignmentId}/submit")
  public ResponseEntity<Void> submit(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID assignmentId) {
    uniqueTaskReviewService.submit(principal.schoolId(), principal.userId(), assignmentId);
    return ResponseEntity.noContent().build();
  }
}
