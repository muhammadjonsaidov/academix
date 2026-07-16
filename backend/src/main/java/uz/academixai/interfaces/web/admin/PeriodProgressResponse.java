package uz.academixai.interfaces.web.admin;

import java.time.LocalDateTime;
import uz.academixai.infrastructure.persistence.GradeRepository.PeriodProgressRow;

public record PeriodProgressResponse(LocalDateTime periodStart, double avgScore, long gradedCount) {

  public static PeriodProgressResponse from(PeriodProgressRow row) {
    return new PeriodProgressResponse(
        row.getPeriodStart(), Math.round(row.getAvgScore() * 10) / 10.0, row.getGradedCount());
  }
}
