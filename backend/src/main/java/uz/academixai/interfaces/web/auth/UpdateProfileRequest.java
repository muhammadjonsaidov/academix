package uz.academixai.interfaces.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Deviation — see {@link ProfileResponse}. Phone is immutable (login identifier). */
public record UpdateProfileRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String firstName,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String lastName,
    @Email(message = "email formati noto'g'ri") @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String email) {}
