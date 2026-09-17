package uz.academixai.interfaces.web.admin;

import java.time.LocalDateTime;
import uz.academixai.reporting.application.port.out.AnalyticsStatistics.PeriodRow;

public record PeriodProgressResponse(LocalDateTime periodStart, double avgScore, long gradedCount) {

  public static PeriodProgressResponse from(PeriodRow row) {
    return new PeriodProgressResponse(
        row.periodStart(), Math.round(row.avgScore() * 10) / 10.0, row.gradedCount());
  }
}
