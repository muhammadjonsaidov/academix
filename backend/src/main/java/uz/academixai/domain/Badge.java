package uz.academixai.domain;

import java.util.UUID;

/**
 * academix_tz.md §4 "checkAndAwardBadges" — plain domain object, no framework annotations. JPA
 * mapping lives in {@code infrastructure.persistence.BadgeEntity}.
 */
public record Badge(
    UUID id,
    String name,
    String description,
    String icon,
    BadgeCriteriaType criteriaType,
    int criteriaValue) {}
