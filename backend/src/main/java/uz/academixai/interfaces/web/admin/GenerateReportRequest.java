package uz.academixai.interfaces.web.admin;

import java.util.UUID;
import uz.academixai.domain.ReportType;

/** academix_tz.md §2.2 {@code POST /admin/reports/generate} — exact body shape. */
public record GenerateReportRequest(ReportType type, String semester, UUID targetId) {}
