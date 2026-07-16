package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.AdminAnalyticsService;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/** academix_tz.md §2.2 "Taqqoslash (faqat admin ko'radi)" — admin-only comparison views. */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

  private final AdminAnalyticsService analyticsService;

  public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping("/classes-comparison")
  public List<ClassProgressResponse> classesComparison(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID subjectId,
      @RequestParam(required = false) String period) {
    return analyticsService.classesComparison(principal.schoolId(), subjectId, period).stream()
        .map(ClassProgressResponse::from)
        .toList();
  }

  @GetMapping("/teachers-ranking")
  public List<TeacherRankingResponse> teachersRanking(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return analyticsService.teachersRanking(principal.schoolId()).stream()
        .map(TeacherRankingResponse::from)
        .toList();
  }

  @GetMapping("/school-progress")
  public List<PeriodProgressResponse> schoolProgress(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) String period) {
    return analyticsService.schoolProgress(principal.schoolId(), period).stream()
        .map(PeriodProgressResponse::from)
        .toList();
  }

  @GetMapping("/ai-usage")
  public AiUsageResponse aiUsage(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) String period) {
    return AiUsageResponse.from(analyticsService.aiUsage(principal.schoolId(), period));
  }
}
