package uz.academixai.reporting.adapter.in.web;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.academixai.reporting.domain.Report;
import uz.academixai.reporting.domain.ReportType;

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
