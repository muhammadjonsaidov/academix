package uz.academixai.interfaces.web.auth;

/** Deviation — see {@link ProfileResponse}. Phone is immutable (login identifier). */
public record UpdateProfileRequest(String firstName, String lastName, String email) {}
