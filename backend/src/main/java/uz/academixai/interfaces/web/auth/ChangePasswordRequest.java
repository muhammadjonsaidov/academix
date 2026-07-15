package uz.academixai.interfaces.web.auth;

public record ChangePasswordRequest(String oldPassword, String newPassword) {}
