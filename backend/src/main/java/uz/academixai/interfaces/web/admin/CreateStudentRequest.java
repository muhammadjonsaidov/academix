package uz.academixai.interfaces.web.admin;

import java.time.LocalDate;
import java.util.UUID;

/**
 * academix_tz.md §2.2 — { firstName, lastName, phone, classId, studentNumber, birthDate }.
 * DEVIATION on top: optional {@code email} and admin-chosen {@code password} — the admin creates
 * every account with its credentials directly (see StudentManagementService). Both may be null
 * (bulk import path keeps the server-generated temp password).
 */
public record CreateStudentRequest(
    String firstName,
    String lastName,
    String phone,
    String email,
    String password,
    UUID classId,
    String studentNumber,
    LocalDate birthDate) {}
