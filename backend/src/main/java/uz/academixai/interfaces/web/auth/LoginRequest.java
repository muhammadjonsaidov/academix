package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A user may authenticate with either their phone number or the email attached to their account.
 */
public record LoginRequest(
    @NotBlank @Size(max = 100) String identifier, @NotBlank @Size(max = 72) String password) {}
