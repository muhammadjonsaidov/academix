package uz.academixai.interfaces.web.psychologist;

import java.util.Map;
import uz.academixai.wellbeing.application.port.in.PsychologistWorkspace.MonthlyReport;

/** academix_tz.md §2.6 — GET /psychologist/reports?period=monthly. Shape is a deviation. */
public record MonthlyReportResponse(
    long totalSignals,
    Map<String, Long> bySeverity,
    Map<String, Long> byType,
    long resolvedCount,
    long manipulationFlaggedCount) {

  public static MonthlyReportResponse from(MonthlyReport report) {
    return new MonthlyReportResponse(
        report.totalSignals(),
        report.bySeverity(),
        report.byType(),
        report.resolvedCount(),
        report.manipulationFlaggedCount());
  }
}
