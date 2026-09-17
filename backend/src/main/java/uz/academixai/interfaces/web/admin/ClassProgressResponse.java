package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.reporting.application.port.out.AnalyticsStatistics.ClassRow;

public record ClassProgressResponse(
    UUID classId, String className, int studentCount, double avgScore, long gradedCount) {

  public static ClassProgressResponse from(ClassRow row) {
    return new ClassProgressResponse(
        row.classId(),
        row.className(),
        row.studentCount(),
        Math.round(row.avgScore() * 10) / 10.0,
        row.gradedCount());
  }
}
