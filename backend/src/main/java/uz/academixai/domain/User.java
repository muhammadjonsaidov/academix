package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.1 — plain domain object, no framework annotations. JPA mapping lives in {@code
 * infrastructure.persistence.UserEntity}.
 */
public record User(
    UUID id,
    String firstName,
    String lastName,
    String phone,
    String email,
    String passwordHash,
    Role role,
    boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime lastLoginAt) {}
