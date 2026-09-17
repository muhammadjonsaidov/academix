package uz.academixai.interfaces.web.psychologist;

import java.util.List;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace.Dashboard;

/** academix_tz.md §2.6 — GET /psychologist/dashboard, exact shape. */
public record PsychologistDashboardResponse(
    long criticalSignals,
    long highSignals,
    long mediumSignals,
    long resolvedThisWeek,
    List<WatchlistStudentResponse> watchlistStudents) {

  public static PsychologistDashboardResponse from(Dashboard dashboard) {
    return new PsychologistDashboardResponse(
        dashboard.criticalSignals(),
        dashboard.highSignals(),
        dashboard.mediumSignals(),
        dashboard.resolvedThisWeek(),
        dashboard.watchlistStudents().stream().map(WatchlistStudentResponse::from).toList());
  }
}
