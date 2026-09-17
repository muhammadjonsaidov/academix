package uz.academixai.reporting.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import uz.academixai.reporting.domain.ReportType;

/** academix_tz.md §2.2 {@code POST /admin/reports/generate} — exact body shape. */
public record GenerateReportRequest(
    @NotNull ReportType type, @NotBlank @Size(max = 20) String quarter, UUID targetId) {}
