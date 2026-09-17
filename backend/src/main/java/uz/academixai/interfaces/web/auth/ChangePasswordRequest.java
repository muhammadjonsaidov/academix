package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String oldPassword,
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String newPassword) {}
