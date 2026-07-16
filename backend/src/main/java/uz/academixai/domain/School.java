package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * academix_tz.md §1.2/§2.2 — full column set (see V1 migration), needed for GET/PUT /admin/school.
 */
public record School(
    UUID id,
    String name,
    String address,
    String region,
    String district,
    String phone,
    String email,
    int totalClasses,
    boolean isActive,
    LocalDateTime subscribedAt,
    LocalDateTime subscriptionEndsAt,
    UUID adminId,
    int monthlyAiCallLimit,
    int currentMonthAiUsage) {}
