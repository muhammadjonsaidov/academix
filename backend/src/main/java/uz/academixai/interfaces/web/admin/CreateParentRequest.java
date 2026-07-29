package uz.academixai.interfaces.web.admin;

/**
 * Phone AND email are both required (email is the password-reset channel); password is the
 * admin-chosen initial password handed to the parent — see ParentManagementService.
 */
public record CreateParentRequest(
    String firstName, String lastName, String phone, String email, String password) {}
