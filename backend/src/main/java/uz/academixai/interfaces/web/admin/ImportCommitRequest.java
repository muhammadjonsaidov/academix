package uz.academixai.interfaces.web.admin;

import java.util.Map;

/** academix_tz.md §2.2 — { fileToken, columnMapping, saveMappingAsTemplate } */
public record ImportCommitRequest(
    String fileToken, Map<String, String> columnMapping, boolean saveMappingAsTemplate) {}
