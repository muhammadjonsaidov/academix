package uz.academixai.interfaces.web.student;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.StudentDashboardService;
import uz.academixai.application.StudentSubmissionService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.4 "Vazifalar" / "Topshirilgan ishlar tarixi" — read-only, exact contract. */
@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentHomeworkController {

  private final StudentSubmissionService submissionService;
  private final StudentDashboardService dashboardService;

  public StudentHomeworkController(
      StudentSubmissionService submissionService, StudentDashboardService dashboardService) {
    this.submissionService = submissionService;
    this.dashboardService = dashboardService;
  }

  @GetMapping("/api/v1/student/dashboard")
  public StudentDashboardResponse getDashboard(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return StudentDashboardResponse.from(
        dashboardService.getDashboard(principal.schoolId(), principal.userId()));
  }

  @GetMapping("/api/v1/student/homework")
  public List<StudentHomeworkResponse> listHomework(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return submissionService.listHomework(principal.schoolId(), principal.userId()).stream()
        .map(StudentHomeworkResponse::from)
        .toList();
  }

  @GetMapping("/api/v1/student/homework/{assignmentId}")
  public StudentHomeworkResponse getHomework(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID assignmentId) {
    return StudentHomeworkResponse.from(
        submissionService.getHomeworkDetail(
            principal.schoolId(), principal.userId(), assignmentId));
  }

  @GetMapping("/api/v1/student/submissions")
  public List<StudentSubmissionListItemResponse> listSubmissions(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return submissionService.listSubmissions(principal.schoolId(), principal.userId()).stream()
        .map(StudentSubmissionListItemResponse::from)
        .toList();
  }

  @GetMapping("/api/v1/student/submissions/{submissionId}")
  public StudentSubmissionDetailResponse getSubmission(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID submissionId) {
    return StudentSubmissionDetailResponse.from(
        submissionService.getSubmissionDetail(
            principal.schoolId(), principal.userId(), submissionId));
  }
}
