package uz.academixai.interfaces.web.admin;

import java.util.List;
import uz.academixai.reporting.application.AdminDashboardService.Dashboard;

/** academix_tz.md §2.2 {@code GET /admin/dashboard} — exact response shape. */
public record AdminDashboardResponse(
    int totalClasses,
    int totalStudents,
    int totalTeachers,
    int totalSubjects,
    int totalAssignments,
    int activeToday,
    double homeworkSubmissionRate,
    List<ClassProgressResponse> classProgressList,
    List<TeacherRankingResponse> teacherRankings,
    PsychAlertsResponse psychologicalAlerts) {

  public record PsychAlertsResponse(int high, int medium) {}

  public static AdminDashboardResponse from(Dashboard dashboard) {
    return new AdminDashboardResponse(
        dashboard.totalClasses(),
        dashboard.totalStudents(),
        dashboard.totalTeachers(),
        dashboard.totalSubjects(),
        dashboard.totalAssignments(),
        dashboard.activeToday(),
        dashboard.homeworkSubmissionRate(),
        dashboard.classProgressList().stream().map(ClassProgressResponse::from).toList(),
        dashboard.teacherRankings().stream().map(TeacherRankingResponse::from).toList(),
        new PsychAlertsResponse(
            dashboard.psychologicalAlerts().high(), dashboard.psychologicalAlerts().medium()));
  }
}
