package uz.academixai.domain;

import java.util.UUID;

/**
 * academix_tz.md §1.4 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.SubjectEntity}.
 */
public record Subject(UUID id, UUID schoolId, String name, SubjectType type, String icon) {}
