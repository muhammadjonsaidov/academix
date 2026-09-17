package uz.academixai.interfaces.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Phone AND email are both required (email is the password-reset channel); password is the
 * admin-chosen initial password handed to the parent — see ParentManagementService.
 */
public record CreateParentRequest(
    @NotBlank @Size(max = 50) String firstName,
    @NotBlank @Size(max = 50) String lastName,
    @NotBlank @Size(max = 20) String phone,
    @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 72) String password) {}
