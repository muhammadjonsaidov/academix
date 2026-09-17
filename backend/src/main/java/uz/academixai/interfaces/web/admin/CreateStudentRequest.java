package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * academix_tz.md §2.2 — { firstName, lastName, phone, classId, studentNumber, birthDate }.
 * DEVIATION on top: optional {@code email} and admin-chosen {@code password} — the admin creates
 * every account with its credentials directly (see StudentManagementService). Both may be null
 * (bulk import path keeps the server-generated temp password).
 */
public record CreateStudentRequest(
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String lastName,
    @NotBlank @Size(max = 20) String phone,
    @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 72) String password,
    @NotNull UUID classId,
    @Size(max = 30) String studentNumber,
    @PastOrPresent LocalDate birthDate) {}
