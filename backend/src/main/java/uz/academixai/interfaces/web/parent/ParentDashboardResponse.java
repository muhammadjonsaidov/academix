package uz.academixai.interfaces.web.parent;

import java.util.List;
import uz.academixai.application.ParentDashboardService.Dashboard;

/** academix_tz.md §2.5 — GET /parent/dashboard, exact shape. */
public record ParentDashboardResponse(List<ChildSummaryResponse> children) {

  public static ParentDashboardResponse from(Dashboard dashboard) {
    return new ParentDashboardResponse(
        dashboard.children().stream().map(ChildSummaryResponse::from).toList());
  }
}
