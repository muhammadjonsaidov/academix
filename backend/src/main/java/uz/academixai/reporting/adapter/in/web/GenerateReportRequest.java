package uz.academixai.reporting.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import uz.academixai.reporting.domain.ReportType;

/** academix_tz.md §2.2 {@code POST /admin/reports/generate} — exact body shape. */
public record GenerateReportRequest(
    @NotNull(message = "majburiy maydon") ReportType type,
    @NotBlank(message = "majburiy maydon") @Size(max = 20, message = "ko'pi bilan 20 belgi")
        String quarter,
    UUID targetId) {}
