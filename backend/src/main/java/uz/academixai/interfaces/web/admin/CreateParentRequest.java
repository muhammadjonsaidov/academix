package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Phone AND email are both required (email is the password-reset channel); password is the
 * admin-chosen initial password handed to the parent — see ParentManagementService.
 */
public record CreateParentRequest(
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String firstName,
    @NotBlank(message = "majburiy maydon") @Size(max = 50, message = "ko'pi bilan 50 belgi")
        String lastName,
    @NotBlank(message = "majburiy maydon") @Size(max = 20, message = "ko'pi bilan 20 belgi")
        String phone,
    @Email(message = "email formati noto'g'ri") @Size(max = 100, message = "ko'pi bilan 100 belgi")
        String email,
    @NotBlank(message = "majburiy maydon") @Size(max = 72, message = "ko'pi bilan 72 belgi")
        String password) {}
