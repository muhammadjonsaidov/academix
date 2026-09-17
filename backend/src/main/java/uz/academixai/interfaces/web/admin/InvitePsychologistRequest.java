package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Deviation, judgment call — see PsychologistManagementService. Password optional (=> active). */
public record InvitePsychologistRequest(
    @NotBlank @Size(max = 20) String phone,
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String lastName,
    @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 72) String password) {}
