package uz.academixai.family.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.7 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.ParentStudentLinkEntity}. No {@code schoolId} field — this table isn't
 * RLS-enabled; callers must scope via the joined {@code student_profiles.school_id}, same shape as
 * grades/psychological_signals.
 */
public record ParentStudentLink(
    UUID id,
    UUID parentUserId,
    UUID studentUserId,
    ParentRelation relation,
    boolean isActive,
    boolean biometricConsentGiven,
    LocalDateTime consentGivenAt) {}
