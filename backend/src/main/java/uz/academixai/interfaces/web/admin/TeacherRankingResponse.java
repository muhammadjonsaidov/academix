package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.reporting.application.port.out.AnalyticsStatistics.TeacherRow;

public record TeacherRankingResponse(
    UUID teacherId, String firstName, String lastName, double avgGrade, long gradedCount) {

  public static TeacherRankingResponse from(TeacherRow row) {
    return new TeacherRankingResponse(
        row.teacherId(),
        row.firstName(),
        row.lastName(),
        Math.round(row.avgGrade() * 10) / 10.0,
        row.gradedCount());
  }
}
