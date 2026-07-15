package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * academix_tz.md §1.24 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.ImportColumnMappingEntity}.
 */
public record ImportColumnMapping(
    UUID id, UUID schoolId, Map<String, String> mapping, LocalDateTime updatedAt) {}
