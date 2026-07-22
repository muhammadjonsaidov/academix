package uz.academixai.interfaces.web.admin;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.domain.Report;
import uz.academixai.domain.ReportType;

public record ReportResponse(
    UUID id,
    ReportType type,
    String quarter,
    UUID targetId,
    UUID generatedBy,
    LocalDateTime generatedAt) {

  public static ReportResponse from(Report report) {
    return new ReportResponse(
        report.id(),
        report.type(),
        report.quarter(),
        report.targetId(),
        report.generatedBy(),
        report.generatedAt());
  }
}
