package uz.academixai.interfaces.web.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public payload accepted only while an installation has no accounts. */
public record InitialSetupRequest(
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String lastName,
    @NotBlank @Size(max = 20) String phone,
    @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 72) String password,
    @NotBlank @Size(max = 150) String schoolName,
    @NotBlank @Size(max = 50) String region,
    @NotBlank @Size(max = 50) String district,
    @NotBlank @Size(max = 500) String address) {}
