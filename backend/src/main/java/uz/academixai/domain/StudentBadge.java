package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §2.4 "GET /student/badges" — plain domain object, no framework annotations. JPA
 * mapping lives in {@code infrastructure.persistence.StudentBadgeEntity}.
 */
public record StudentBadge(UUID id, UUID studentId, UUID badgeId, LocalDateTime awardedAt) {}
