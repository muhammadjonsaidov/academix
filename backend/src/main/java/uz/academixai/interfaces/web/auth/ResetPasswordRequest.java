package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @Size(max = 255) String token, @NotBlank @Size(max = 72) String newPassword) {}
