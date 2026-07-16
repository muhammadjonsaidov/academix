package uz.academixai.interfaces.web.admin;

/** Deviation, judgment call — see PsychologistManagementService. */
public record InvitePsychologistRequest(
    String phone, String firstName, String lastName, String email) {}
