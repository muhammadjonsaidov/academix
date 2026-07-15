package uz.academixai.interfaces.web.admin;

/** academix_tz.md §2.2 — { phone: "+998...", firstName, lastName, email? } */
public record InviteTeacherRequest(String phone, String firstName, String lastName, String email) {}
