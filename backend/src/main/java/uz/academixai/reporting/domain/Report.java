package uz.academixai.reporting.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §2.2 {@code POST/GET /admin/reports} — no DDL exists in the spec (see V36
 * migration's comment); plain domain object, no framework annotations.
 */
public record Report(
    UUID id,
    UUID schoolId,
    ReportType type,
    String quarter,
    UUID targetId,
    String fileUrl,
    UUID generatedBy,
    LocalDateTime generatedAt) {}
