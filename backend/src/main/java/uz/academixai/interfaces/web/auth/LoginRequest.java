package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A user may authenticate with either their phone number or the email attached to their account.
 */
public record LoginRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String identifier,
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String password) {}
