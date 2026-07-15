package uz.academixai.domain;

import java.util.UUID;

/**
 * academix_tz.md §1.3 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.SchoolClassEntity}.
 */
public record SchoolClass(
    UUID id,
    UUID schoolId,
    int grade,
    String letter,
    String fullName,
    UUID classTeacherId,
    int studentCount,
    String academicYear,
    boolean isActive) {}
