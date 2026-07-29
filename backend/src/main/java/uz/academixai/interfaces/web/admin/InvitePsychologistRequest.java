package uz.academixai.interfaces.web.admin;

/** Deviation, judgment call — see PsychologistManagementService. Password optional (=> active). */
public record InvitePsychologistRequest(
    String phone, String firstName, String lastName, String email, String password) {}
