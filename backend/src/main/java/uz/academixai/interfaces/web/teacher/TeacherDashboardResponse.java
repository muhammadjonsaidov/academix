package uz.academixai.interfaces.web.teacher;

import java.util.List;
import uz.academixai.application.TeacherDashboardService.Dashboard;

/** academix_tz.md §2.3 {@code GET /teacher/dashboard} — exact response shape. */
public record TeacherDashboardResponse(
    List<TeacherClassResponse> myClasses,
    int pendingSubmissions,
    long gradedToday,
    MyRating myRating,
    List<TeacherClassProgressResponse> classProgressSummary) {

  public record MyRating(double score, String trend) {}

  public static TeacherDashboardResponse from(Dashboard dashboard) {
    return new TeacherDashboardResponse(
        dashboard.myClasses().stream().map(TeacherClassResponse::from).toList(),
        dashboard.pendingSubmissions(),
        dashboard.gradedToday(),
        new MyRating(dashboard.myRating().score(), dashboard.myRating().trend()),
        dashboard.classProgressSummary().stream().map(TeacherClassProgressResponse::from).toList());
  }
}
