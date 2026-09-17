package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Email is the recovery identifier: users can recover access even when their phone is unavailable.
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "majburiy maydon")
        @Email(message = "email formati noto'g'ri")
        @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String email) {}
