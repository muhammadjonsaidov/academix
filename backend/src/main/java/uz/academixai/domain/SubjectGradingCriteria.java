package uz.academixai.domain;

import java.util.List;
import java.util.UUID;

/**
 * academix_tz.md §1.19 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.SubjectGradingCriteriaEntity}.
 */
public record SubjectGradingCriteria(
    UUID id, UUID subjectId, UUID teacherId, List<CriteriaItem> criteria) {}
