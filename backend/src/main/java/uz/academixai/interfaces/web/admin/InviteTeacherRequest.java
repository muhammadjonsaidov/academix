package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * academix_tz.md §2.2 — { phone: "+998...", firstName, lastName, email? }. DEVIATION on top:
 * optional admin-chosen {@code password} — present = account active immediately (see
 * TeacherManagementService).
 */
public record InviteTeacherRequest(
    @NotBlank @Size(max = 20) String phone,
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String lastName,
    @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 72) String password) {}
