package uz.academixai.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/** academix_tz.md §3.4/§8 — plain domain object, no framework annotations. */
public record AiChatMessage(
    UUID id,
    UUID schoolId,
    UUID studentId,
    SubjectType subject,
    String message,
    String response,
    boolean isBlocked,
    LocalDateTime createdAt) {}
