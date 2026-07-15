package uz.academixai.interfaces.web.admin;

import java.util.List;

/** academix_tz.md §2.2 — POST /admin/students/bulk-import/commit response. */
public record ImportCommitResponse(
    int totalRows, int imported, int failed, List<ImportRowError> errors) {}
