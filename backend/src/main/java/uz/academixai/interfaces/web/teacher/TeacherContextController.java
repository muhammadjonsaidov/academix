package uz.academixai.interfaces.web.teacher;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.TeacherAnalyticsService;
import uz.academixai.application.TeacherDashboardService;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.school.application.port.in.TeacherAccess;

/** academix_tz.md §2.3 "Mening sinflarim va fanlarim" — exact contract, don't drift. */
@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherContextController {

  private final TeacherAccess teacherAccess;
  private final TeacherDashboardService teacherDashboardService;
  private final TeacherAnalyticsService teacherAnalyticsService;

  public TeacherContextController(
      TeacherAccess teacherAccess,
      TeacherDashboardService teacherDashboardService,
      TeacherAnalyticsService teacherAnalyticsService) {
    this.teacherAccess = teacherAccess;
    this.teacherDashboardService = teacherDashboardService;
    this.teacherAnalyticsService = teacherAnalyticsService;
  }

  @GetMapping("/dashboard")
  public TeacherDashboardResponse dashboard(@AuthenticationPrincipal AcademixPrincipal principal) {
    return TeacherDashboardResponse.from(
        teacherDashboardService.dashboard(principal.schoolId(), principal.userId()));
  }

  @GetMapping("/students/{studentId}/progress")
  public TeacherStudentProgressResponse studentProgress(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID studentId) {
    return TeacherStudentProgressResponse.from(
        teacherAnalyticsService.studentProgress(
            principal.schoolId(), principal.userId(), studentId));
  }

  @GetMapping("/classes/{classId}/analytics")
  public TeacherClassAnalyticsResponse classAnalytics(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID classId) {
    return TeacherClassAnalyticsResponse.from(
        teacherAnalyticsService.classAnalytics(principal.schoolId(), principal.userId(), classId));
  }

  @GetMapping("/classes")
  public List<TeacherClassResponse> classes(@AuthenticationPrincipal AcademixPrincipal principal) {
    return teacherAccess.myClasses(principal.schoolId(), principal.userId()).stream()
        .map(TeacherClassResponse::from)
        .toList();
  }

  @GetMapping("/subjects")
  public List<TeacherSubjectResponse> subjects(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return teacherAccess.mySubjects(principal.schoolId(), principal.userId()).stream()
        .map(TeacherSubjectResponse::from)
        .toList();
  }

  @GetMapping("/classes/{classId}/students")
  public List<TeacherStudentResponse> classStudents(
      @AuthenticationPrincipal AcademixPrincipal principal, @PathVariable UUID classId) {
    return teacherAccess.classStudents(principal.schoolId(), principal.userId(), classId).stream()
        .map(TeacherStudentResponse::from)
        .toList();
  }
}
