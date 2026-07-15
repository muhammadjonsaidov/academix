package uz.academixai.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * academix_backend_tdd.md §4.1 — plain domain object, no framework annotations. JPA mapping lives
 * in {@code infrastructure.persistence.StudentProfileEntity}.
 */
public record StudentProfile(
    UUID id,
    UUID userId,
    UUID classId,
    UUID schoolId,
    String studentNumber,
    LocalDate birthDate,
    int totalXp,
    int currentStreak,
    int maxStreak,
    LocalDate lastSubmissionDate,
    boolean isActive) {}
