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
import uz.academixai.domain.SubmissionStatus;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.interfaces.web.PageResponse;
import uz.academixai.learning.application.port.in.HomeworkGrading;
import uz.academixai.learning.application.port.in.TeacherSubmissionQuery;

/** academix_tz.md §2.3 "Topshirilgan ishlarni ko'rish va baholash" — exact contract. */
@RestController
@RequestMapping("/api/v1/teacher/submissions")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherSubmissionController {

  private final TeacherSubmissionQuery submissionService;
  private final HomeworkGrading gradingService;

  public TeacherSubmissionController(
      TeacherSubmissionQuery submissionService, HomeworkGrading gradingService) {
    this.submissionService = submissionService;
    this.gradingService = gradingService;
  }

  @GetMapping
  public PageResponse<TeacherSubmissionListItemResponse> list(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID assignmentId,
      @RequestParam(required = false) UUID classId,
      @RequestParam(required = false) SubmissionStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    List<TeacherSubmissionListItemResponse> all =
        submissionService
            .list(principal.schoolId(), principal.userId(), assignmentId, classId, status)
            .stream()
            .map(TeacherSubmissionListItemResponse::from)
            .toList();
    return PageResponse.slice(all, page, size);
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
        gradingService.grade(
            principal.schoolId(),
            principal.userId(),
            submissionId,
            request.score(),
            request.fivePointGrade(),
            request.teacherComment(),
            request.isExcellent());
    return TeacherGradeResponse.from(saved);
  }
}
