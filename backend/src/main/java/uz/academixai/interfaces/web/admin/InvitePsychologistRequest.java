package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Deviation, judgment call — see PsychologistManagementService. Password optional (=> active). */
public record InvitePsychologistRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 20, message = "ko'pi bilan 20 belgi")
        String phone,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String firstName,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String lastName,
    @Email(message = "email formati noto'g'ri") @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String email,
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String password) {}
