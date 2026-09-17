package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Deviation — see {@link ProfileResponse}. Phone is immutable (login identifier). */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String lastName,
    @Email @Size(max = 100) String email) {}
