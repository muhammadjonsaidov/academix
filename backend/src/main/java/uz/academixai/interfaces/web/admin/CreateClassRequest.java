package uz.academixai.interfaces.web.admin;

import java.util.UUID;

/** academix_tz.md §2.2 — { grade: 7, letter: "A", classTeacherId: "uuid" } */
public record CreateClassRequest(int grade, String letter, UUID classTeacherId) {}
