package uz.academixai.interfaces.web.auth;

/**
 * A user may authenticate with either their phone number or the email attached to their account.
 */
public record LoginRequest(String identifier, String password) {}
