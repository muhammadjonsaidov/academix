package uz.academixai.interfaces.web.student;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.StudentDashboardService;
import uz.academixai.application.StudentSubmissionService;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.interfaces.web.PageResponse;
import uz.academixai.progress.application.StudentProgressService;

/** academix_tz.md §2.4 "Vazifalar" / "Topshirilgan ishlar tarixi" — read-only, exact contract. */
@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentHomeworkController {

  private final StudentSubmissionService submissionService;
  private final StudentDashboardService dashboardService;
  private final StudentProgressService progressService;

  public StudentHomeworkController(
      StudentSubmissionService submissionService,
      StudentDashboardService dashboardService,
      StudentProgressService progressService) {
    this.submissionService = submissionService;
    this.dashboardService = dashboardService;
    this.progressService = progressService;
  }

  @GetMapping("/api/v1/student/progress")
  public StudentProgressResponse getProgress(@AuthenticationPrincipal AcademixPrincipal principal) {
    return StudentProgressResponse.from(
        progressService.progress(principal.schoolId(), principal.userId()));
  }

  @GetMapping("/api/v1/student/dashboard")
  public StudentDashboardResponse getDashboard(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return StudentDashboardResponse.from(
        dashboardService.getDashboard(principal.schoolId(), principal.userId()));
  }

  @GetMapping("/api/v1/student/badges")
  public List<BadgeResponse> listBadges(@AuthenticationPrincipal AcademixPrincipal principal) {
    return dashboardService.listBadges(principal.schoolId(), principal.userId()).stream()
        .map(BadgeResponse::from)
        .toList();
  }

  @GetMapping("/api/v1/student/xp-history")
  public PageResponse<XpHistoryResponse> listXpHistory(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    List<XpHistoryResponse> all =
        dashboardService.listXpHistory(principal.schoolId(), principal.userId()).stream()
            .map(XpHistoryResponse::from)
            .toList();
    return PageResponse.slice(all, page, size);
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
