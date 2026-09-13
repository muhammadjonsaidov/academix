package uz.academixai.interfaces.web.parent;

import java.util.List;
import uz.academixai.application.ParentDashboardService.ChildOverview;
import uz.academixai.learning.application.port.in.StudentHomeworkQuery.HomeworkItem;

/** Response shape is a deviation — §2.5 gives the endpoint path, not the body. */
public record ChildOverviewResponse(
    ChildSummaryResponse summary,
    List<HomeworkItem> pendingHomework,
    List<ChildSummaryResponse.RecentGradeResponse> recentGrades) {

  public static ChildOverviewResponse from(ChildOverview overview) {
    return new ChildOverviewResponse(
        ChildSummaryResponse.from(overview.summary()),
        overview.pendingHomework(),
        overview.recentGrades().stream()
            .map(ChildSummaryResponse.RecentGradeResponse::from)
            .toList());
  }
}
