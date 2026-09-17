package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 255, message = "ko'pi bilan 255 belgi")
        String token,
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String newPassword) {}
