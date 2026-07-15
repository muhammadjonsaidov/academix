package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.TeacherSubmissionService;
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.3 "Topshirilgan ishlarni ko'rish va baholash" — exact contract. */
@RestController
@RequestMapping("/api/v1/teacher/submissions")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherSubmissionController {

  private final TeacherSubmissionService submissionService;

  public TeacherSubmissionController(TeacherSubmissionService submissionService) {
    this.submissionService = submissionService;
  }

  @GetMapping
  public List<TeacherSubmissionListItemResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID assignmentId,
      @RequestParam(required = false) UUID classId,
      @RequestParam(required = false) SubmissionStatus status) {
    return submissionService
        .list(principal.schoolId(), principal.userId(), assignmentId, classId, status)
        .stream()
        .map(TeacherSubmissionListItemResponse::from)
        .toList();
  }

  @GetMapping("/{submissionId}")
  public TeacherSubmissionDetailResponse get(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID submissionId) {
    return TeacherSubmissionDetailResponse.from(
        submissionService.get(principal.schoolId(), principal.userId(), submissionId));
  }

  @PostMapping("/{submissionId}/grade")
  public TeacherGradeResponse grade(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @PathVariable UUID submissionId,
      @RequestBody GradeSubmissionRequest request) {
    var saved =
        submissionService.grade(
            principal.schoolId(),
            principal.userId(),
            submissionId,
            request.score(),
            request.fivePointGrade(),
            request.teacherComment());
    return TeacherGradeResponse.from(saved);
  }
}
