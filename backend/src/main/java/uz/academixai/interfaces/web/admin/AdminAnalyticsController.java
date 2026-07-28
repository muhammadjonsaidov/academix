package uz.academixai.interfaces.web.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.application.AdminAnalyticsService;
import uz.academixai.infrastructure.cache.RedisJsonCache;
import uz.academixai.infrastructure.security.AcademixPrincipal;

/**
 * academix_tz.md §2.2 "Taqqoslash (faqat admin ko'radi)" — admin-only comparison views.
 *
 * <p>The three aggregation endpoints are Redis-cached for 2 minutes per school+params — these are
 * multi-join GROUP BY scans whose result an admin dashboard re-requests on every visit, and a
 * 2-minute staleness window is invisible for "how did classes do this month" data. Cached at the
 * response-DTO layer (concrete records round-trip through Jackson; the repository's interface
 * projections don't).
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

  private static final Duration CACHE_TTL = Duration.ofMinutes(2);

  private final AdminAnalyticsService analyticsService;
  private final RedisJsonCache cache;

  public AdminAnalyticsController(AdminAnalyticsService analyticsService, RedisJsonCache cache) {
    this.analyticsService = analyticsService;
    this.cache = cache;
  }

  @GetMapping("/classes-comparison")
  public List<ClassProgressResponse> classesComparison(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) UUID subjectId,
      @RequestParam(required = false) String period) {
    String key = "analytics:classes:%s:%s:%s".formatted(principal.schoolId(), subjectId, period);
    return cache.getOrLoad(
        key,
        CACHE_TTL,
        new TypeReference<>() {},
        () ->
            analyticsService.classesComparison(principal.schoolId(), subjectId, period).stream()
                .map(ClassProgressResponse::from)
                .toList());
  }

  @GetMapping("/teachers-ranking")
  public List<TeacherRankingResponse> teachersRanking(
      @AuthenticationPrincipal AcademixPrincipal principal) {
    return cache.getOrLoad(
        "analytics:teachers:%s".formatted(principal.schoolId()),
        CACHE_TTL,
        new TypeReference<>() {},
        () ->
            analyticsService.teachersRanking(principal.schoolId()).stream()
                .map(TeacherRankingResponse::from)
                .toList());
  }

  @GetMapping("/school-progress")
  public List<PeriodProgressResponse> schoolProgress(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) String period) {
    return cache.getOrLoad(
        "analytics:progress:%s:%s".formatted(principal.schoolId(), period),
        CACHE_TTL,
        new TypeReference<>() {},
        () ->
            analyticsService.schoolProgress(principal.schoolId(), period).stream()
                .map(PeriodProgressResponse::from)
                .toList());
  }

  @GetMapping("/ai-usage")
  public AiUsageResponse aiUsage(
      @AuthenticationPrincipal AcademixPrincipal principal,
      @RequestParam(required = false) String period) {
    return AiUsageResponse.from(analyticsService.aiUsage(principal.schoolId(), period));
  }
}
