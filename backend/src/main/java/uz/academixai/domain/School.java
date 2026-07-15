package uz.academixai.domain;

import java.util.UUID;

/**
 * academix_tz.md §1.2 — minimal slice for now (just enough to resolve admin's schoolId at login,
 * plus the AI budget cap); extend with the remaining fields when admin school-settings endpoints
 * are built.
 */
public record School(UUID id, String name, UUID adminId, int monthlyAiCallLimit) {}
