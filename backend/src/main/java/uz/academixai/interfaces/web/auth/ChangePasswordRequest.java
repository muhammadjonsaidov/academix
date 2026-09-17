package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank @Size(max = 72) String oldPassword, @NotBlank @Size(max = 72) String newPassword) {}
