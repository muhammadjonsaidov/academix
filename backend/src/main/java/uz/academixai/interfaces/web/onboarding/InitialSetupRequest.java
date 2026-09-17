package uz.academixai.interfaces.web.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public payload accepted only while an installation has no accounts. */
public record InitialSetupRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String firstName,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String lastName,
    @NotBlank(message = "majburiy maydon") @Size(max = 20, message = "ko'pi bilan 20 belgi")
        String phone,
    @Email(message = "email formati noto'g'ri") @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String email,
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String password,
    @NotBlank(message = "majburiy maydon") @Size(max = 150, message = "ko'pi bilan 150 belgi")
        String schoolName,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String region,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String district,
    @NotBlank(message = "majburiy maydon") @Size(max = 500, message = "ko'pi bilan 500 belgi")
        String address) {}
