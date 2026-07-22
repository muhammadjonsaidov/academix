package uz.academixai.interfaces.web.auth;

public record ResetPasswordRequest(String token, String newPassword) {}
