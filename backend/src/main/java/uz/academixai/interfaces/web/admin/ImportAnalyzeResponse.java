package uz.academixai.interfaces.web.admin;

import java.util.List;
import java.util.Map;

/** academix_tz.md §2.2 — POST /admin/students/bulk-import/analyze response. */
public record ImportAnalyzeResponse(
    String fileToken,
    List<String> detectedColumns,
    Map<String, String> suggestedMapping,
    List<Map<String, String>> previewRows) {}
