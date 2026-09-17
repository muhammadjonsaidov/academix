package uz.academixai.interfaces.web.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.academixai.infrastructure.security.AcademixPrincipal;
import uz.academixai.reporting.application.AdminDashboardService;

/** academix_tz.md §2.2 — admin dashboard summary. */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

  private final AdminDashboardService dashboardService;

  public AdminDashboardController(AdminDashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping
  public AdminDashboardResponse dashboard(@AuthenticationPrincipal AcademixPrincipal principal) {
    return AdminDashboardResponse.from(dashboardService.dashboard(principal.schoolId()));
  }
}
