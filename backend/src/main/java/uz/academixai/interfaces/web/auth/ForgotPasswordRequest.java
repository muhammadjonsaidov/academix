package uz.academixai.interfaces.web.auth;

/** Email is the recovery identifier: users can recover access even when their phone is unavailable. */
public record ForgotPasswordRequest(String email) {}
